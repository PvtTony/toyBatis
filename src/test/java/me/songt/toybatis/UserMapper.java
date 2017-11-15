package me.songt.toybatis;

import me.songt.toybatis.annotation.Mapping;
import me.songt.toybatis.annotation.QueryParamId;
import me.songt.toybatis.annotation.ResultEntity;
import me.songt.toybatis.mapper.BaseMapper;

import java.util.List;

@Mapping(filename = "user.json")
@ResultEntity(entityClass = User.class)
public interface UserMapper extends BaseMapper<User>
{
    List<User> findAllUser();

    List<User> findByNickname(@QueryParamId(queryParamId = "nick") String nick);

    List<User> findByEmailAndNickname(@QueryParamId(queryParamId = "email") String email, @QueryParamId(queryParamId = "nick") String nickname);
}
