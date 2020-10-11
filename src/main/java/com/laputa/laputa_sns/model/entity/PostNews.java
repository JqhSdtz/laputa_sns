package com.laputa.laputa_sns.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 动态
 * @author JQH
 * @since 下午 1:39 20/03/11
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class PostNews extends AbstractBaseEntity {

    private String entityType = "PSNS";

    /**
     * 动态接受者ID
     */
    @JsonProperty("receiver_id")
    private Integer receiverId;

    /**
     * 动态发送者ID
     */
    @JsonProperty("sender_id")
    private Integer senderId;

    /**
     * 动态对应的帖子的ID
     */
    @JsonProperty("post_id")
    private Integer postId;

    /**
     * 动态对应的帖子
     */
    private Post content;

    public PostNews(Integer id) {
        this.id = id;
    }

    public Integer getOfId() {
        return receiverId;
    }

    @JsonIgnore
    public boolean isValidReadNewsParam() {
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return false;
        return true;
    }
}
