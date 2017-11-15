package me.songt.toybatis.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbSessionFactory
{
    public static DbSession createSession(String propertyFilename)
    {
        Properties properties = new Properties();
        InputStream inputStream = DbSessionFactory.class.getClassLoader().getResourceAsStream(propertyFilename);
        try
        {
            if (inputStream != null)
            {
                properties.load(inputStream);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            if (inputStream != null)
            {
                try
                {
                    inputStream.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        String jdbcUrl = properties.getProperty("jdbc.url");
        String jdbcUsername = properties.getProperty("jdbc.username");
        String jdbcPassword = properties.getProperty("jdbc.password");
        DbSession session = new DbSession(jdbcUrl, jdbcUsername, jdbcPassword);
        return session;
    }
}
