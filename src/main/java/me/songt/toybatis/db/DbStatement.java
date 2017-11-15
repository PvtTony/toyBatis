package me.songt.toybatis.db;


import java.sql.PreparedStatement;
import java.util.Map;

public class DbStatement
{
    private String operation;
    private PreparedStatement preparedStatement;
    private Map<String, Integer> paramMap;

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public PreparedStatement getPreparedStatement()
    {
        return preparedStatement;
    }

    public void setPreparedStatement(PreparedStatement preparedStatement)
    {
        this.preparedStatement = preparedStatement;
    }

    public Map<String, Integer> getParamMap()
    {
        return paramMap;
    }

    public void setParamMap(Map<String, Integer> paramMap)
    {
        this.paramMap = paramMap;
    }
}
