package com.laputa.laputa_sns.right;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author JQH
 * @since 上午 10:39 20/04/05
 */

@Getter
@Setter
public class CategoryRight {
    @JsonProperty("this_level")
    private Integer thisLevel;
    @JsonProperty("parent_level")
    private Integer parentLevel;
    private Boolean create;
    private Boolean delete;
    @JsonProperty("update_info")
    private Boolean updateInfo;
    @JsonProperty("update_disp_seq")
    private Boolean updateDispSeq;
    @JsonProperty("update_cache_num")
    private Boolean updatePreCachedNum;
    @JsonProperty("update_parent")
    private Boolean updateParent;
    @JsonProperty("top_post")
    private Boolean topPost;
    @JsonProperty("def_sub")
    private Boolean defSub;
}
