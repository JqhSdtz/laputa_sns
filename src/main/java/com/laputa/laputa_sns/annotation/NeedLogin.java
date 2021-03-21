package com.laputa.laputa_sns.annotation;

import java.lang.annotation.*;

/**
 * @author JQH
 * @since 下午 1:00 21/03/14
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NeedLogin {
}
