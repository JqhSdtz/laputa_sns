package com.laputa.laputa_sns.annotation;

import java.lang.annotation.*;

/**
 * @author JQH
 * @since 下午 10:00 21/02/28
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(AccessLimits.class)
public @interface AccessLimit {
    int value();
    LimitTimeUnit per() default LimitTimeUnit.MINUTE;
}
