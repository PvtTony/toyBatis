package me.songt.toybatis.handler;

import me.songt.toybatis.annotation.Column;
import me.songt.toybatis.annotation.Identity;
import me.songt.toybatis.annotation.QueryParamId;
import me.songt.toybatis.annotation.Table;
import me.songt.toybatis.db.DbStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapperInvocationHandler implements InvocationHandler
{
    private Logger logger = LoggerFactory.getLogger(MapperInvocationHandler.class);
    private final Connection connection;
    private final Map<String, DbStatement> mappingStatements;
    private final Class<?> targetEntityClass;
    public MapperInvocationHandler(Connection connection, Map<String, DbStatement> mappingStatements,
                                   Class<?> targetEntityClass)
    {
        this.connection = connection;
        this.mappingStatements = mappingStatements;
        this.targetEntityClass = targetEntityClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        String methodName = method.getName();
        logger.debug(String.format("Method name: %s", methodName));

        String tableName = targetEntityClass.getAnnotation(Table.class).tableName();
        switch (methodName)
        {
            case "save":
                return saveMethod(tableName, args[0]);
            case "update":
                return updateMethod(tableName, args[0]);
            case "delete":
                return deleteMethod(tableName, args[0]);
            default:
                if (mappingStatements.containsKey(methodName))
                {

                    DbStatement dbStatement = mappingStatements.get(methodName);
                    String operation = dbStatement.getOperation();
                    Map<String, Integer> paramMap = dbStatement.getParamMap();
                    PreparedStatement preparedStatement = dbStatement.getPreparedStatement();
                    if (method.getParameterCount() > 0)
                    {
                        Parameter[] parameters = method.getParameters();
                        for (int i = 0, j = 0; i < parameters.length; i++)
                        {
                            Parameter parameter = parameters[i];
                            if (parameter.isAnnotationPresent(QueryParamId.class))
                            {
                                String queryId = parameter.getAnnotation(QueryParamId.class).queryParamId();
                                Integer index = paramMap.get(queryId);
                                preparedStatement.setObject(index, args[j]);
                                j++;
                            }
                        }
                    }
                    switch (operation)
                    {
                        case "select":
                            ResultSet selectResult = preparedStatement.executeQuery();
                            return fillResultList(targetEntityClass, selectResult);
                        default:
                            return preparedStatement.executeUpdate() > 0;
                    }
                }
                break;
        }
        return null;
    }

    private <T> Iterable<T> fillResultList(Class<T> target, ResultSet resultSet) throws ClassNotFoundException
    {
        List<T> resultList = null;
        try
        {
            Field[] fields = target.getDeclaredFields();
            if (resultSet != null)
            {
                resultList = new ArrayList<>();
                while (resultSet.next())
                {
                    T object = target.newInstance();
                    for (Field field : fields)
                    {
                        if (field.isAnnotationPresent(Column.class))
                        {
                            Column column = field.getAnnotation(Column.class);
                            String columnName = column.columnName();
                            Object columnData = resultSet.getObject(columnName);
                            String fieldName = field.getName();
                            String finalFieldName = fieldName.substring(0, 1).toUpperCase()
                                    .concat(fieldName.substring(1, fieldName.length()));
                            Method method = object.getClass().getMethod("set" + finalFieldName, field.getType());
                            method.invoke(object, columnData);
                        }
                    }
                    resultList.add(object);
                }
            }
        } catch (SQLException | IllegalAccessException | InstantiationException | NoSuchMethodException
                | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        return resultList;
    }

    private Object saveMethod(String tableName, Object object) throws NoSuchMethodException, SQLException, InvocationTargetException, IllegalAccessException
    {
        StringBuilder columnSqlBuilder = new StringBuilder();
        StringBuilder valueSqlBuilder = new StringBuilder();
        columnSqlBuilder.append("(");
        valueSqlBuilder.append("VALUES (");
        Field[] fields = object.getClass().getDeclaredFields();
        List<Object> objectList = new ArrayList<>();
        Method primarySetterMethod = null;

        for (Field field : fields)
        {
            if (field.isAnnotationPresent(Identity.class))
            {
                String setterMethodName = getFieldSetterMethodName(field.getName());
                primarySetterMethod = object.getClass().getMethod(setterMethodName, field.getType());
            }
            String fieldName = field.getName();
            String columnName = field.getAnnotation(Column.class).columnName();
            String getMethodName  = getFieldGetterMethodName(fieldName);
            Method getMethod = object.getClass().getMethod(getMethodName);
            Object fieldData = getMethod.invoke(object);
            columnSqlBuilder.append(columnName);
            columnSqlBuilder.append(',');
            valueSqlBuilder.append("?,");
            objectList.add(fieldData);
            logger.debug(String.format("Arg field name: %s, column name: %s", fieldName, columnName));
        }
        columnSqlBuilder.deleteCharAt(columnSqlBuilder.length() - 1);
        valueSqlBuilder.deleteCharAt(valueSqlBuilder.length() - 1);
        columnSqlBuilder.append(')');
        valueSqlBuilder.append(')');
        String columnSql = columnSqlBuilder.toString();
        String valueSql = valueSqlBuilder.toString();
        String saveSQL = String.format("INSERT INTO %s %s %s", tableName, columnSql, valueSql);
        logger.debug(saveSQL);
        PreparedStatement preparedStatement = connection.prepareStatement(saveSQL, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < objectList.size(); i++)
        {
            preparedStatement.setObject(i + 1, objectList.get(i));
        }

        if (preparedStatement.executeUpdate() > 0)
        {
            ResultSet keyResult = preparedStatement.getGeneratedKeys();
            if (keyResult.next())
            {
                primarySetterMethod.invoke(object, keyResult.getInt(1));
            }
        }
        return object;
    }

    private boolean updateMethod(String tableName, Object object) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, SQLException
    {
        Field[] fields = object.getClass().getDeclaredFields();
        String primaryCondition = null;
        Object primaryData = null;
        List<Object> objectList = new ArrayList<>();
        StringBuilder setData = new StringBuilder();
        setData.append("SET ");
        for (int i = 0; i < fields.length; i++)
        {
            Field field = fields[i];
            if (field.isAnnotationPresent(Column.class))
            {
                String columnName = field.getAnnotation(Column.class).columnName();
                String getterMethodName = getFieldGetterMethodName(field.getName());
                Method method = object.getClass().getMethod(getterMethodName);
                Object fieldData = method.invoke(object);
                if (field.isAnnotationPresent(Identity.class))
                {
                    primaryCondition = String.format("%s = ?", columnName);
                    primaryData = fieldData;
                }
                else
                {
                    setData.append(String.format("%s = ?,", columnName));
                    objectList.add(fieldData);
                }
            }
        }
        setData.deleteCharAt(setData.length() - 1);
        if (primaryCondition == null || primaryCondition.isEmpty())
        {
            return false;
        }
        String updateSql = String.format("UPDATE %s %s WHERE %s", tableName, setData.toString(), primaryCondition);
        logger.debug(updateSql);
        PreparedStatement statement = connection.prepareStatement(updateSql);
        statement.setObject(objectList.size() + 1, primaryData);
        for (int i = 0; i < objectList.size(); i++)
        {
            statement.setObject(i + 1, objectList.get(i));
        }
        return statement.executeUpdate() > 0;
    }

    private boolean deleteMethod(String tableName, Object object) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, SQLException
    {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields)
        {
            if (field.isAnnotationPresent(Column.class) && field.isAnnotationPresent(Identity.class))
            {
                String columnName = field.getAnnotation(Column.class).columnName();
                String getterMethodName = getFieldGetterMethodName(columnName);
                Method getterMethod = object.getClass().getMethod(getterMethodName);
                Object identityData = getterMethod.invoke(object);
                String deleteSql = String.format("DELETE FROM %s WHERE %s = ?", tableName, columnName);
                PreparedStatement preparedStatement = connection.prepareStatement(deleteSql);
                preparedStatement.setObject(1, identityData);
                return preparedStatement.execute();
            }
        }
        return false;
    }

    private String getFieldGetterMethodName(String fieldName)
    {
        return String.format("get%s",
            fieldName.substring(0, 1).toUpperCase().concat(fieldName.substring(1, fieldName.length())));
    }

    private String getFieldSetterMethodName(String fieldName)
    {
        return String.format("set%s",
                fieldName.substring(0, 1).toUpperCase().concat(fieldName.substring(1, fieldName.length())));
    }
}
