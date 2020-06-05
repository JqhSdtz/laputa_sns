package com.laputa.laputa_sns.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author JQH
 * @since 下午 1:39 20/03/11
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class PostNews extends AbstractBaseEntity {

    private String entityType = "PSNS";

    @JsonProperty("receiver_id")
    private Integer receiverId;

    @JsonProperty("sender_id")
    private Integer senderId;

    @JsonProperty("post_id")
    private Integer postId;

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
