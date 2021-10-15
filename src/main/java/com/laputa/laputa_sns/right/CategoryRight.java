package com.laputa.laputa_sns.right;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 目录操作权限
 * @author JQH
 * @since 上午 10:39 20/04/05
 */

@Getter
@Setter
@Accessors(chain = true)
public class CategoryRight {
    /**
     * 本目录的权限等级
     */
    @JsonProperty("this_level")
    private Integer thisLevel;
    /**
     * 父目录的权限登录
     */
    @JsonProperty("parent_level")
    private Integer parentLevel;
    /**
     * 是否有创建目录权限
     */
    private Boolean create;
    /**
     * 是否有删除目录权限
     */
    private Boolean delete;
    /**
     * 是否有更新目录信息权限
     */
    @JsonProperty("update_info")
    private Boolean updateInfo;
    /**
     * 是否有更新目录在父目录展示顺序权限
     */
    @JsonProperty("update_disp_seq")
    private Boolean updateDispSeq;
    /**
     * 是否有更新目录的预缓存数量权限
     */
    @JsonProperty("update_cache_num")
    private Boolean updatePreCachedNum;
    /**
     * 是否有更新目录的父目录权限
     */
    @JsonProperty("update_parent")
    private Boolean updateParent;
    /**
     * 是否有更新目录的置顶贴子权限
     */
    @JsonProperty("top_post")
    private Boolean topPost;

    /**
     * 是否有设置发帖允许管理等级权限
     */
    @JsonProperty("update_allow_post_level")
    private Boolean updateAllowPostLevel;

    @JsonProperty("talk_ban")
    private Boolean talkBan;

    @JsonProperty("create_editable_post")
    private Boolean createEditablePost;
    /**
     * 是否有更新目录的默认子目录权限
     */
    @JsonProperty("def_sub")
    private Boolean defSub;
}
