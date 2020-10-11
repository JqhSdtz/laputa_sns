package com.laputa.laputa_sns.right;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author JQH
 * @since 上午 10:07 20/04/05
 */

@Getter
@Setter
public class PostRight {
    private Boolean top;
    /**
     * 是否有置顶该帖子的权限
     */
    @JsonProperty("be_topped")
    private Boolean beTopped;
    /**
     * 是否有删除该帖子的权限
     */
    private Boolean delete;
}
