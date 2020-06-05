package com.laputa.laputa_sns.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author JQH
 * @since 下午 9:46 20/04/19
 */

@Getter
@Setter
@Accessors(chain = true)
public class Notice {
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

    private Integer type;
    @JsonProperty("content_id")
    private Integer contentId;
    @JsonProperty("unread_cnt")
    private Long unreadCnt;
    @JsonProperty("init_time")
    private Date initTime;
    @JsonProperty("update_time")
    private Date updateTime;

    private Object content;

    @JsonProperty("type_str")
    public String getTypeStr() {
        return typeStr[type];
    }
}
