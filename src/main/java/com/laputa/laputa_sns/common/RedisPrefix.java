package com.laputa.laputa_sns.common;

/**
 * Redis中Key的前缀的统一管理类
 * @author JQH
 * @since 下午 7:53 20/02/18
 */
public class  RedisPrefix {
    public static final String ONLINE_OPERATOR = "OP";//在线用户

    public static final String CATEGORY_COUNTER = "CG/CNT";//目录计数器

    public static final String USER_NAME_IDX_HASH = "US/NH";//保存所有用户的用户名和ID的映射，不删除
    public static final String USER_BASIC = "US/BS";//用户基本信息
    public static final String USER_RECV_SETTING = "US/RCVSTN";//用户接收通知的设置
    public static final String USER_COUNTER = "US/CNT";//用户关注和粉丝计数器
    public static final String USER_NEWS_IN_BOX = "US/NIB";//缓存用户接收的动态
    public static final String USER_NEWS_OUT_BOX_INIT = "US/NOB/INIT";//用户发件箱初始化标志
    public static final String USER_NEWS_OUT_BOX = "US/NOB";//用户新发表的帖子，用于推送
    public static final String USER_NOTICE_BOX_TIME = "US/NTB/T";//用户接收到的消息，保存每条的更新时间
    public static final String USER_NOTICE_BOX_CNT = "US/NTB/C";//用户接收到的消息，保存每条的计数
    public static final String USER_NOTICE_BOX_INIT_TIME = "US/NTB/I";//用户接收到的消息，保存每条的创建时间
    public static final String USER_FOLLOWING_LIST = "US/FLN";//用户关注列表
    public static final String USER_FOLLOWER_LIST = "US/FLR";//用户粉丝列表
    public static final String USER_LAST_REFRESH_TIME = "US/LRTH";//用户最近刷新动态的时间映射
    public static final String USER_RECENT_VISIT_CATEGORY = "US/VIS";//用户最近访问的目录

    public static final String LIKE_RECORD_SET_NEW = "LKS/N";//新增点赞记录的集合，用来检查是否存在某条记录
    public static final String LIKE_RECORD_SET_FORMER = "LKS/F";//之前点赞记录的集合，用来缓存数据

    public static final String POST_BASIC = "PS/BS";//帖子基本信息
    public static final String POST_CONTENT = "PS/CTT";//帖子内容信息
    public static final String POST_FULL_TEXT = "PS/FTX";//帖子全文信息
    public static final String POST_COUNTER = "PS/CNT";//帖子一级评论计数器

    public static final String FORWARD_RECORD_SET = "FW/IDX";//帖子的转发索引，每日清空

    public static final String CML1_INDEX_OF_POST = "C1/IDX";//帖子的一级评论索引，每日清空
    public static final String CML1_BASIC = "C1/BS";//一级评论基本信息
    public static final String CML1_CONTENT = "C1/CTT";//一级评论内容信息
    public static final String CML1_COUNTER = "C1/CNT";//一级评论二级评论计数器

    public static final String CML2_INDEX_OF_CML1 = "C2/IDX";//二级评论的二级评论索引，每日清空
    public static final String CML2_BASIC = "C2/BS";//一级评论基本信息
    public static final String CML2_CONTENT = "C2/CTT";//二级评论内容信息
}
