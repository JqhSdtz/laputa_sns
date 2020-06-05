package com.laputa.laputa_sns.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.right.PostRight;
import com.laputa.laputa_sns.service.CategoryService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

/**
 * @author JQH
 * @since 下午 2:43 20/02/05
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonFilter("PostFilter")
public class Post extends AbstractContent<Post> {

    public static final int TYPE_PUBLIC = 0;
    public static final int TYPE_FORWARD = 1;
    public static final int TYPE_PRIVATE = 2;

    public static final int OF_CATEGORY = 1;
    public static final int OF_CREATOR = 2;
    public static final int OF_SUP_POST = 3;

    private String entityType = "POST";

    private Integer ofType;

    @JsonIgnore
    private String fullText;
    @JsonProperty("full_text_id")
    private Integer fullTextId;
    private Integer queryFullText;

    private String title;
    private String content;
    @JsonProperty("raw_img")
    private String rawImg;
    private Integer length;

    @JsonProperty("allow_forward")
    private Integer allowForward;

    @JsonProperty("sup_id")
    private Integer supId;
    @JsonProperty("ori_id")
    private Integer oriId;

    @JsonProperty("ori_post")
    private Post oriPost;

    @JsonIgnore
    private Category category;

    @JsonProperty("like_cnt")
    private Long likeCnt;
    @JsonProperty("comment_cnt")
    private Long commentCnt;
    @JsonProperty("forward_cnt")
    private Long forwardCnt;
    @JsonProperty("view_cnt")
    private Long viewCnt;

    @JsonProperty("liked_by_viewer")
    private Boolean likedByViewer;

    @JsonProperty("is_topped")
    private Boolean isTopped;

    @JsonProperty("latest_time")
    private Date latestTime;

    @JsonProperty("top_comment_id")
    private Integer topCommentId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User creator;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PostRight rights;

    @JsonProperty("preview_comment_list")
    private List<CommentL1> previewCommentList;

    @JsonIgnore
    private Boolean lIndexedFlag;

    @JsonIgnore
    private Boolean pIndexedFlag;

    public Post(Integer id) {
        this.id = id;
    }

    @JsonProperty("type_str")
    public String getTypeStr() {
        if (type.equals(Post.TYPE_PUBLIC))
            return "public";
        if (type.equals(Post.TYPE_FORWARD))
            return "forward";
        if (type.equals(Post.TYPE_PRIVATE))
            return "private";
        return "unknown";
    }

    @JsonProperty("category_id")
    public Integer getCategoryId() {
        return category == null ? null : category.getId();
    }

    public Integer getCategoryIdForRight() {
        return (type == null || !type.equals(TYPE_PUBLIC)) ? CategoryService.GROUND_ID : getCategoryId();
    }

    @JsonProperty("category_path")
    public List<Category> getCategoryPath() {
        return category == null ? null : category.getPathList();
    }

    @JsonProperty("parent_id")
    public Integer getParentId() {
        if (ofType == null || ofType == OF_CATEGORY)
            return getCategoryId();
        if (ofType == OF_CREATOR)
            return getCreatorId();
        if (ofType == OF_SUP_POST)
            return getSupId();
        return null;
    }

    @JsonProperty("parent_id")
    public Post setParentId(Integer id) {
        if (ofType == null || ofType == OF_CATEGORY)
            return setCategoryId(id);
        if (ofType == OF_CREATOR)
            return setCreatorId(id);
        if (ofType == OF_SUP_POST)
            return setSupId(id);
        return this;
    }

    @JsonIgnore
    public AbstractContent getParent() {
        return null;
    }

    @JsonIgnore
    public Long getChildNum() {
        return getCommentCnt();
    }

    @JsonProperty("creator_id")
    public Integer getCreatorId() {
        return creator == null ? null : creator.getId();
    }

    @JsonProperty("category_id")
    public Post setCategoryId(Integer id) {
        if (category == null)
            category = new Category();
        category.setId(id);
        return this;
    }

    @JsonProperty("creator_id")
    public Post setCreatorId(Integer id) {
        if (creator == null)
            creator = new User();
        creator.setId(id);
        return this;
    }

    @JsonIgnore
    public Post setEntityContent(@NotNull Post post) {
        this.title = post.title;
        this.content = post.content;
        this.rawImg = post.rawImg;
        return this;
    }

    @JsonIgnore
    public boolean validatePostContent() {
        if (title != null && title.length() > 20)
            return false;
        if (content == null || content.length() < 10 || content.length() > 100000)
            return false;
        if (rawImg != null && rawImg.length() > 256)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidInsertParam() {
        if (type == null)
            return false;
        if (type.equals(TYPE_PUBLIC) && (category == null || category.getId() == null))
            return false;
        return validatePostContent();
    }

    @JsonIgnore
    public boolean isValidInsertForwardParam() {
        if (content == null || supId == null)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidDeleteParam() {
        return id != null;
    }

    @JsonIgnore
    public boolean isValidSetTopCommentParam() {
        return id != null && topCommentId != null;
    }

    @JsonIgnore
    public boolean isValidCancelTopCommentParam() {
        return id != null;
    }

    @JsonIgnore
    public boolean isValidReadIndexOfCategoryParam() {
        if (supId != null || oriId != null || allowForward != null || type != null || state != null || creator != null)
            return false;
        if (category == null || category.getId() == null)
            return false;
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidReadIndexOfCreatorParam() {
        if (supId != null || oriId != null || allowForward != null || type != null || state != null || category != null)
            return false;
        if (creator == null || creator.getId() == null)
            return false;
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidReadForwardListParam() {
        if (supId == null)
            return false;
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return false;
        return true;
    }

}
