package me.songt.toybatis;

import me.songt.toybatis.annotation.Column;
import me.songt.toybatis.annotation.Identity;
import me.songt.toybatis.annotation.Table;

import java.sql.Types;

@Table(tableName = "user")
public class User
{
    @Identity
    @Column(columnName = "id", columnType = Types.INTEGER)
    private int id;
    @Column(columnName = "user_email")
    private String email;
    @Column(columnName = "user_pass")
    private String pass;
    @Column(columnName = "user_nick")
    private String nickname;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPass()
    {
        return pass;
    }

    public void setPass(String pass)
    {
        this.pass = pass;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    @Override
    public String toString()
    {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", pass='" + pass + '\'' +
                ", nickname='" + nickname + '\'' +
                '}';
    }
}
