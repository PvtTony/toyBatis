package me.songt.toybatis;

import me.songt.toybatis.db.DbSession;
import me.songt.toybatis.db.DbSessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class UserTest
{

    private static DbSession dbSession;

    private static UserMapper userMapper;

    private static User testUser;

    @BeforeClass
    public static void initTest() throws IOException, SQLException
    {
        dbSession = DbSessionFactory.createSession("database.properties");
        userMapper = dbSession.createMapper(UserMapper.class);
        testUser = new User();
        testUser.setId(0);
        testUser.setNickname("yst2");
        testUser.setEmail("test");
        testUser.setPass("1234");
    }

    @Test
    public void insertTest()
    {
        testUser = userMapper.save(testUser);
        assertNotEquals(0, testUser.getId());
    }

    @Test
    public void updateTest()
    {
        testUser.setPass("123456");
        userMapper.update(testUser);
        assertEquals("123456", testUser.getPass());
    }

    @Test
    public void selectTest()
    {
        List<User> userList = userMapper.findByEmailAndNickname("yu@songt.me", "yst2");
        assertEquals(1, userList.size());
    }

    @AfterClass
    public static void remove()
    {
        userMapper.delete(testUser);
        dbSession.shutdown();
    }
}
