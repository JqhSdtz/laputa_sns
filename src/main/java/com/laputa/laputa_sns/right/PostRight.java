package com.laputa.laputa_sns.right;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 帖子操作权限
 * @author JQH
 * @since 上午 10:07 20/04/05
 */

@Getter
@Setter
public class PostRight {
    /**
     * 是否有置顶评论的权限
     */
    private Boolean topComment;
    /**
     * 是否有置顶该帖子的权限
     */
    @JsonProperty("be_topped")
    private Boolean beTopped;

    /**
     * 是否有编辑帖子的权限
     */
    private Boolean edit;
    /**
     * 是否有删除该帖子的权限
     */
    private Boolean delete;
}
