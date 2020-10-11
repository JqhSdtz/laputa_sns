package com.laputa.laputa_sns.right;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 评论操作权限
 * @author JQH
 * @since 上午 11:27 20/04/05
 */

@Getter
@Setter
@Accessors(chain = true)
public class CommentRight {
    /**
     * 是否有置顶该评论的权限
     */
    @JsonProperty("be_topped")
    private Boolean beTopped;
    /**
     * 是否有删除该评论的权限
     */
    private Boolean delete;
}
