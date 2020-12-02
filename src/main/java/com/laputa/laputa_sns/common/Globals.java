package com.laputa.laputa_sns.common;

/**
 * 全局变量和相关方法
 * @author JQH
 * @since 下午 1:17 20/03/28
 */
public class Globals {
    public static boolean PostIndexInitialized = false;
    public static boolean CategoryServiceInitialized = false;
    public static boolean UserNameInitialized = false;
    public static boolean UserNewsBoxInitialized = false;

    public static boolean ready() {
        return PostIndexInitialized && CategoryServiceInitialized && UserNameInitialized
                && UserNewsBoxInitialized;
    }
}
