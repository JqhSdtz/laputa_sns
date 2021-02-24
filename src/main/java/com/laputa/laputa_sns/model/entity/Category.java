package com.laputa.laputa_sns.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import com.laputa.laputa_sns.common.IndexList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 目录
 * @author JQH
 * @since 下午 2:43 20/02/05
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class Category extends AbstractBaseEntity implements Cloneable {

    /**
     * 普通目录，向上层聚集
     */
    public static final int TYPE_COMMON = 0;
    /**
     * 私有目录，不向上层聚集
     */
    public static final int TYPE_PRIVATE = 1;
    /**
     * 默认子目录，向上层聚集
     */
    public static final int TYPE_DEF_SUB = 2;

    private String entityType = "CATEGORY";

    /**
     * 目录名称
     */
    private String name;
    /**
     * 目录介绍
     */
    private String intro;
    /**
     * 封面图片地址
     */
    @JsonProperty("cover_img")
    private String coverImg;

    /**
     * 图标图片地址
     */
    @JsonProperty("icon_img")
    private String iconImg;
    /**
     * 本目录在父目录的展示顺序，由前端解析
     */
    @JsonProperty("disp_seq")
    private Integer dispSeq;

    /**
     * 目录的帖子数，仅用作数据传输，在传输前读取oriPostCnt和缓存中的数量，临时计算并赋值
     */
    @JsonProperty("post_cnt")
    private Long postCnt;

    /**
     * 目录的原始帖子数，即不算缓存的数量
     */
    @JsonIgnore
    private Long oriPostCnt;

    /**
     * 目录缓存的帖子数量，用作初始化时获取加载的数量，以及刷新缓存时确定要驻留的数量
     */
    @JsonProperty("cache_num")
    private Integer cacheNum;

    /**
     * 该目录的默认子目录，由前端解析，作用是选择目录的时候，如果不选子目录，则默认选中该目录，目前前端尚未处理该字段
     */
    @JsonProperty("def_sub_id")
    private Integer defSubId;

    /**
     * 该目录的置顶帖子ID
     */
    @JsonProperty("top_post_id")
    private Integer topPostId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User creator;

    @JsonIgnore
    private Category parent;
    /**
     * 子目录列表
     */
    @JsonIgnore
    private List<Category> subCategoryList = new ArrayList();
    /**
     * 新帖列表
     */
    @JsonIgnore
    private IndexList latestPostList;
    /**
     * 热帖列表
     */
    @JsonIgnore
    private IndexList popularPostList;

    /**
     * 该目录的路径，即从根目录到该目录的所有目录列表
     */
    @JsonProperty("path_list")
    private List<Category> pathList;

    /**
     * 是否是叶目录
     */
    @JsonProperty("is_leaf")
    private Boolean isLeaf;

    public Category(Integer id) {
        this.id = id;
    }

    @JsonProperty("parent_id")
    public Integer getParentId() {
        return parent == null ? null : parent.id;
    }

    @JsonProperty("parent_id")
    public Category setParentId(Integer id) {
        if (parent == null)
            parent = new Category();
        parent.id = id;
        return this;
    }

    @JsonProperty("popular_cache_num")
    public Integer getPopularCacheNum() {
        return popularPostList == null ? null : popularPostList.size();
    }

    @JsonProperty("latest_cache_num")
    public Integer getLatestCacheNum() {
        return latestPostList == null ? null : latestPostList.size();
    }

    @JsonIgnore
    /**用于在Category对象作为参数对象的时候判断是否合法*/
    public boolean isValidUpdateInfoParam(boolean format) {
        if (id == null)
            return false;//id不能为空
        if (format) {
            this.setPostCnt(null).setParent(null).setTopPostId(null).setDefSubId(null).setDispSeq(null)
                    .setCacheNum(null);
        } else {
            if (postCnt != null || parent != null || topPostId != null || defSubId != null || dispSeq != null
                    || cacheNum != null)//这些不属于info的范畴，需特殊判断
                return false;
        }
        if (name == null && intro == null && coverImg == null && iconImg == null && type == null && state == null)
            return false;//这些不能全空
        return true;
    }

    @JsonIgnore
    public boolean isValidUpdateParentParam() {
        return id != null && parent != null && parent.id != null;
    }

    @JsonIgnore
    public boolean isValidUpdateDispSeqParam() {
        return id != null && dispSeq != null;
    }

    @JsonIgnore
    public boolean isValidUpdateCacheNumParam() {
        // 禁用手动更新预缓存值
//        return id != null && cacheNum != null && cacheNum >= 100 && cacheNum <= 10000;
        return false;
    }

    @JsonIgnore
    public boolean isValidSetTopPostParam() {
        return id != null && topPostId != null;
    }

    @JsonIgnore
    public boolean isValidSetDefSubParam() {
        return id != null && defSubId != null;
    }

    @JsonIgnore
    /**用于在Category对象作为新添加对象的时候判断是否合法*/
    public boolean isValidInsertParam(boolean format) {
        if (name == null || parent == null || parent.id == null)
            return false;
        if (format) {
            this.setPostCnt(null).setTopPostId(null).setDefSubId(null).setDispSeq(null);
        } else {
            if (postCnt != null || topPostId != null || defSubId != null || dispSeq != null)//过滤掉新添加数据不应该出现的字段
                return false;
        }
        return true;
    }

    @JsonIgnore
    public boolean isParentOf(@NotNull Category sub) {
        do {
            sub = sub.parent;
        } while (sub != null && !this.id.equals(sub.id));
        return sub != null;//sub != null 说明 this.id.equals(sub.id)
    }

    public Category copyUpdateInfoParam(@NotNull Category param) {
        this.name = param.name == null ? this.name : param.name;
        this.type = param.type == null ? this.type : param.type;
        this.state = param.state == null ? this.state : param.state;
        this.intro = param.intro == null ? this.intro : param.intro;
        this.coverImg = param.coverImg == null ? this.coverImg : param.coverImg;
        this.iconImg = param.iconImg == null ? this.iconImg : param.iconImg;
        return this;
    }

    @SneakyThrows
    @Override
    public Category clone() {
        return (Category) super.clone();
    }

}
