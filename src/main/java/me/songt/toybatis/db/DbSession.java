package me.songt.toybatis.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.songt.toybatis.annotation.Mapping;
import me.songt.toybatis.annotation.ResultEntity;
import me.songt.toybatis.entity.Mapper;
import me.songt.toybatis.entity.Params;
import me.songt.toybatis.entity.Query;
import me.songt.toybatis.handler.MapperInvocationHandler;
import me.songt.toybatis.mapper.BaseMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class DbSession
{
    private Logger logger = LoggerFactory.getLogger(DbSession.class);
    private Connection connection;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private Gson gson = new Gson();
    /*public DbSession()
    {
        init();
    }
*/

    public DbSession(String jdbcUrl, String jdbcUsername, String jdbcPassword)
    {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;

        init();
    }

    private void init()
    {
        logger.info("Initializing");
        try
        {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            if (jdbcUrl != null && jdbcUsername != null && jdbcPassword != null)
            {
                String fullUrl = String.format("%s?user=%s&password=%s", jdbcUrl, jdbcUsername, jdbcPassword);
                connection = DriverManager.getConnection(fullUrl);
            }
            else
            {
                logger.error("Incorrect configuration.");
            }
        } catch (InstantiationException | IllegalAccessException | SQLException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    /*public ResultSet select(String sql)
    {
        if (connection != null)
        {
            try
            {
                PreparedStatement statement = connection.prepareStatement(sql);
                return statement.executeQuery();
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }*/

    private List<Mapper> getMapperFileData(Class<? extends Object> mapper) throws IOException
    {
        if (mapper.isAnnotationPresent(Mapping.class))
        {
            Mapping mapping = mapper.getAnnotation(Mapping.class);
            String mappingFilename = mapping.filename();
            String mappingContent = IOUtils.resourceToString(mappingFilename, null,
                    DbSession.class.getClassLoader());
            if (mappingContent != null && !mappingContent.isEmpty())
            {
                Type mapperListType = new TypeToken<ArrayList<Mapper>>(){}.getType();
                List<Mapper> mapperList = gson.fromJson(mappingContent, mapperListType);
                return mapperList;
            }
        }
        return null;
    }

    private Map<String, DbStatement> mapping2DbStatement(List<Mapper> mappers) throws SQLException
    {
        Map<String, DbStatement> resultMapping = null;
        if (mappers != null && mappers.size() > 0)
        {
            resultMapping = new HashMap<>();
            for (Mapper mapper : mappers)
            {
                Query query = mapper.getQuery();
                String operation = query.getOperation();
                List<Params> paramsList = query.getParams();
                DbStatement dbStatement = new DbStatement();
                Map<String, Integer> paramMap = new HashMap<>();
                Map<Integer, String> rawParam = new TreeMap<>();
                dbStatement.setOperation(operation);
                if (paramsList != null && paramsList.size() > 0)
                {
                    String statement = query.getStatement();

                    for (Params param : paramsList)
                    {
                        String target = String.format("#{%s}", param.getId());
                        int index = statement.indexOf(target);
                        rawParam.put(index, param.getId());
                        statement = statement.replace(target, "?");
//                        paramMap.put(param.getId(), param.getIndex());
                    }

                    Iterator paramIterator = rawParam.entrySet().iterator();
                    int index = 1;
                    while (paramIterator.hasNext())
                    {
                        Map.Entry entry = (Map.Entry) paramIterator.next();
                        String paramId = (String) entry.getValue();
                        paramMap.put(paramId, index);
                        index++;
                    }

                    PreparedStatement preparedStatement = connection.prepareStatement(statement);
                    dbStatement.setPreparedStatement(preparedStatement);
                    dbStatement.setParamMap(paramMap);
                    logger.debug(statement);
                }
                else
                {
                    dbStatement.setPreparedStatement(connection.prepareStatement(query.getStatement()));
                }
                resultMapping.put(mapper.getId(), dbStatement);
            }
        }
        return resultMapping;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseMapper> T createMapper(Class<T> mapperClass) throws IOException, SQLException
    {
        List<Mapper> mapperList = getMapperFileData(mapperClass);
        Map<String, DbStatement> dbStatementMap = mapping2DbStatement(mapperList);
        if (!mapperClass.isAnnotationPresent(ResultEntity.class))
        {
            return null;
        }
        T mapper = (T) Proxy.newProxyInstance(DbSession.class.getClassLoader(), new Class[]{mapperClass},
                new MapperInvocationHandler(connection,
                        dbStatementMap,
                        mapperClass.getAnnotation(ResultEntity.class).entityClass()));
        return mapper;
    }

    /*public <T> List<T> fillResultList(Class<T> target, ResultSet resultSet) throws ClassNotFoundException
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
    }*/

    public void shutdown()
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }
}
