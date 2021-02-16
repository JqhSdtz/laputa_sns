package com.laputa.laputa_sns.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 消息，即用户收到的消息，其表示某个内容收到了某个类型的操作，并记录数量
 * 比如某个帖子获得了10个点赞、或某个一级评论获得了10条二级评论
 * @author JQH
 * @since 下午 9:46 20/04/19
 */

@Getter
@Setter
@Accessors(chain = true)
public class Notice extends AbstractBaseEntity {
    public static final int TYPE_LIKE_POST = 0;
    public static final int TYPE_LIKE_CML1 = 1;
    public static final int TYPE_LIKE_CML2 = 2;
    public static final int TYPE_CML1_OF_POST = 3;
    public static final int TYPE_CML2_OF_CML1 = 4;
    public static final int TYPE_FW_POST = 5;
    public static final int TYPE_FOLLOWER = 6;
    public static final int TYPE_REPLY_OF_CML2 = 7;

    private static final String[] typeStr = {"like_post", "like_cml1", "like_cml2", "cml1_of_post", "cml2_of_cml1",
            "fw_post", "follower", "reply_of_cml2"};

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 消息的目标内容的ID，可能是某个帖子、评论的ID
     */
    @JsonProperty("content_id")
    private Integer contentId;

    /**
     * 消息内容，可能是某个帖子、评论的内容
     */
    private Object content;

    /**
     * 消息的目标内容所包含的未读数量
     */
    @JsonProperty("unread_cnt")
    private Long unreadCnt;

    /**
     * 消息的创建时间
     */
    @JsonProperty("init_time")
    private Date initTime;

    /**
     * 消息的更新时间
     */
    @JsonProperty("update_time")
    private Date updateTime;

    @JsonProperty("type_str")
    public String getTypeStr() {
        return typeStr[type];
    }

    @JsonIgnore
    public boolean isValidPullNoticeParam() {
        if (queryParam.getFrom() == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return false;
        return true;
    }
}
