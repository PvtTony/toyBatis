package me.songt.toybatis.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResultEntity
{
//    String className() default "";
    Class<?> entityClass();
}
