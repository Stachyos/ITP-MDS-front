package org.help789.mds.logging;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAction {
    String value();                 // 操作名
    String detail() default "";     // 可选 SpEL 描述，如 "删除ID=#{#id}"
}
