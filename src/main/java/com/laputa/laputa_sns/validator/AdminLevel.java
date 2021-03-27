package com.laputa.laputa_sns.validator;

/**
 * @author JQH
 * @since 下午 1:23 20/04/23
 */
public class AdminLevel {
    public static final int SUPER_ADMIN = 99;

    //删除帖子，需要本目录权限
    public static final int DELETE_CATEGORY_POST = 1;
    // 修改帖子目录，需要本目录权限
    public static final int UPDATE_POST_CATEGORY = 3;
    //禁言，需要根目录权限
    public static final int TALK_BAN = 1;
    //设置置顶帖，需要本目录权限
    public static final int SET_CATEGORY_TOP_POST = 2;
    // 设置允许发帖管理等级，需要本目录权限
    public static final int SET_ALLOW_POST_LEVEL = 3;
    // 发布可修改帖子，需要本目录权限
    public static final int CREATE_EDITABLE_POST = 1;
    //设置默认子目录，需要本目录权限
    public static final int SET_CATEGORY_DEF_SUB = 2;
    //更新目录信息，需要本目录权限
    public static final int UPDATE_CATEGORY_INFO = 2;
    //更改展示顺序，需要父目录权限
    public static final int UPDATE_CATEGORY_DISP_SEQ = 3;
    //更新预加载数量，需要本目录权限
    public static final int UPDATE_CATEGORY_PRE_CACHED_NUM = 3;
    //更新父目录，需要本目录权限和目标目录的创建子目录权限
    public static final int UPDATE_CATEGORY_PARENT = 4;
    //创建目录，需要父目录权限
    public static final int CREATE_CATEGORY = 4;
    //删除目录，强制删除需要父目录权限，非强制删除仅需本目录权限
    public static final int DELETE_CATEGORY = 4;
}
