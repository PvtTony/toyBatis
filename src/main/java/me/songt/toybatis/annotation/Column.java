package me.songt.toybatis.annotation;

import java.lang.annotation.*;
import java.sql.Types;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column
{
    String columnName() default "";
    int columnType() default Types.VARCHAR;
}
