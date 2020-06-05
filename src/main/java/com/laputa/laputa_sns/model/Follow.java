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
 * @since 下午 5:51 20/02/20
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class Follow extends AbstractBaseEntity {

    private User follower;
    private User target;

    private String entityType = "FOLLOW";

    @JsonProperty("follower_id")
    public Follow setFollowerId(Integer id) {
        if (follower == null)
            follower = new User();
        follower.setId(id);
        return this;
    }

    @JsonProperty("target_id")
    public Follow setTargetId(Integer id) {
        if (target == null)
            target = new User();
        target.setId(id);
        return this;
    }

    public Follow(Integer id) {
        this.id = id;
    }

    @JsonProperty("follower_id")
    public Integer getFollowerId() {
        return follower == null ? null : follower.getId();
    }

    @JsonProperty("target_id")
    public Integer getTargetId() {
        return target == null ? null : target.getId();
    }

    @JsonIgnore
    public boolean isValidUpdateParam() {
        if(id == null && target == null)
            return false;
        if(type == null)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidInsertParam() {
        if(target == null || type == null)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidDeleteParam() {
        if(id == null && target == null)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidReadIndexOfTargetParam() {
        if (target == null || target.getId() == null)
            return false;
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return false;
        return true;
    }
}
