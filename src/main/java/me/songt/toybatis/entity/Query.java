package me.songt.toybatis.entity;

import java.util.List;

public class Query
{
    private String operation;
    private String statement;
    private List<Params> params;

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public String getStatement()
    {
        return statement;
    }

    public void setStatement(String statement)
    {
        this.statement = statement;
    }

    public List<Params> getParams()
    {
        return params;
    }

    public void setParams(List<Params> params)
    {
        this.params = params;
    }
}
