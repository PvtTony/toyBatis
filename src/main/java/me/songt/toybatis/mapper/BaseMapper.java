package me.songt.toybatis.mapper;

public interface BaseMapper<T>
{
    T save(T object);

    void delete(T object);

    T update(T object);
}
