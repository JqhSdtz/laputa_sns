package com.laputa.laputa_sns.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author JQH
 * @since 下午 3:08 20/02/24
 */

@Getter
@Setter
@Accessors(chain = true)
/**type为0表示对帖子的赞，1表示一级评论，2表示二级评论*/
public class LikeRecord extends AbstractBaseEntity {

    public static final int TYPE_POST = 0;
    public static final int TYPE_CML1 = 1;
    public static final int TYPE_CML2 = 2;

    private String entityType = "LIKE";

    @JsonProperty("target_id")
    private Integer targetId;

    private User creator;

    @JsonProperty("creator_id")
    public Integer getCreatorId() {
        return creator == null ? null : creator.getId();
    }

    @JsonProperty("creator_id")
    public LikeRecord setCreatorId(Integer id) {
        if (creator == null)
            creator = new User();
        creator.setId(id);
        return this;
    }

    @JsonIgnore
    public boolean isValidTargetListSelectParam() {
         if(targetId == null || type == null)
             return false;
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidInsertParam() {
        return targetId != null && type != null;
    }

    @JsonIgnore
    public boolean isValidDeleteParam() {
        return targetId != null && type != null;
    }
}
