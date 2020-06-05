package com.laputa.laputa_sns.model;

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
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author JQH
 * @since 下午 2:43 20/02/05
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class Category extends AbstractBaseEntity implements Cloneable {

    public static final int TYPE_COMMON = 0;//普通目录
    public static final int TYPE_PRIVATE = 1;//不向上层聚集的目录
    public static final int TYPE_DEF_SUB = 2;//默认子目录

    private String entityType = "CATEGORY";

    private String name;
    private String intro;
    @JsonProperty("cover_img")
    private String coverImg;
    @JsonProperty("disp_seq")
    private Integer dispSeq;

    @JsonProperty("post_cnt")
    private Long postCnt;

    @JsonIgnore
    private Long oriPostCnt;

    @JsonProperty("cache_num")
    private Integer cacheNum;

    @JsonProperty("def_sub_id")
    private Integer defSubId;

    @JsonProperty("top_post_id")
    private Integer topPostId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User creator;

    @JsonIgnore
    private Category parent;
    @JsonIgnore
    private List<Category> subCategoryList = new ArrayList();
    @JsonIgnore
    private IndexList latestPostList;
    @JsonIgnore
    private IndexList popularPostList;

    @JsonProperty("path_list")
    private List<Category> pathList;

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
    public boolean isValidUpdateInfoParam() {
        if (id == null)
            return false;//id不能为空
        if (postCnt != null || parent != null || topPostId != null || defSubId != null || dispSeq != null
                || cacheNum != null)//这些不属于info的范畴，需特殊判断
            return false;
        if (name == null && intro == null && coverImg == null && type == null && state == null)
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
        return id != null && cacheNum != null && cacheNum >= 100 && cacheNum <= 10000;
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
    public boolean isValidInsertParam() {
        if (name == null || parent == null || parent.id == null)
            return false;
        if (postCnt != null || topPostId != null || defSubId != null || dispSeq != null)//过滤掉新添加数据不应该出现的字段
            return false;
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
        return this;
    }

    @SneakyThrows
    @Override
    public Category clone() {
        return (Category) super.clone();
    }

}
