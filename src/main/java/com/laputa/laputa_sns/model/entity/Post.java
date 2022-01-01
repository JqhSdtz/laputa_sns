package com.laputa.laputa_sns.model.entity;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.right.PostRight;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

/**
 * 帖子
 *
 * @author JQH
 * @since 下午 2:43 20/02/05
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonFilter("PostFilter")
public class Post extends AbstractContent<Post> {

    /**
     * 公开的帖子，会出现在所发表的目录，个人主页以及粉丝的动态中
     */
    public static final Integer TYPE_PUBLIC = 0;

    /**
     * 转发的帖子，会出现在个人主页以及粉丝的动态中
     */
    public static final Integer TYPE_FORWARD = 1;

    /**
     * 非公开的帖子，会出现在个人主页以及粉丝的动态中
     */
    public static final Integer TYPE_PRIVATE = 2;

    /**
     * 该帖子对象从属于一个目录，即处于查询某个目录的帖子的请求中
     */
    public static final int OF_CATEGORY = 1;
    /**
     * 该帖子对象从属于一个用户，即处于查询某个用户所发表的帖子的请求中
     */
    public static final int OF_CREATOR = 2;
    /**
     * 该帖子对象从属于一个被转发的帖子，即处于查询某个帖子的转发帖子的请求中
     */
    public static final int OF_SUP_POST = 3;

    private String entityType = "POST";

    private Integer ofType;

    /**
     * 帖子的完整内容，短帖子没有该字段
     */
    @JsonProperty("full_text")
    private String fullText;
    /**
     * 帖子的完整内容ID，完整内容和帖子不在一个表中存储
     */
    @JsonProperty("full_text_id")
    private Integer fullTextId;
    /**
     * 是否查询完整内容
     */
    private Integer queryFullText;

    /**
     * 帖子标题
     */
    private String title;
    /**
     * 帖子内容(可能不是完整内容)
     */
    private String content;
    /**
     * 帖子图片的原始字段值，即多个路径拼成的字符串
     */
    @JsonProperty("raw_img")
    private String rawImg;
    /**
     * 帖子内容长度(完整内容)
     */
    private Integer length;

    /**
     * 帖子是否允许转发
     */
    @JsonProperty("allow_forward")
    private Integer allowForward;

    /**
     * 帖子的上级帖子，即被转发帖子(仅在该帖是转发帖的时候存在)
     */
    @JsonProperty("sup_id")
    private Integer supId;
    /**
     * 帖子的原始帖子ID，即一次或多次转发的时候，最初的帖子(仅在该帖是转发帖的时候存在)
     */
    @JsonProperty("ori_id")
    private Integer oriId;
    /**
     * 帖子的原始帖子
     */
    @JsonProperty("ori_post")
    private Post oriPost;

    /**
     * 帖子所属目录
     */
    @JsonIgnore
    private Category category;

    /**
     * 帖子点赞数
     */
    @JsonProperty("like_cnt")
    private Long likeCnt;
    /**
     * 帖子评论数(一级评论)
     */
    @JsonProperty("comment_cnt")
    private Long commentCnt;
    /**
     * 帖子转发数
     */
    @JsonProperty("forward_cnt")
    private Long forwardCnt;
    /**
     * 帖子查看数，当前未使用
     */
    @JsonProperty("view_cnt")
    private Long viewCnt;

    /**
     * 当前登录用户是否给该帖子点赞，仅在数据传输时使用
     */
    @JsonProperty("liked_by_viewer")
    private Boolean likedByViewer;

    /**
     * 当前帖子是否被置顶，仅在数据传输时使用
     */
    @JsonProperty("is_topped")
    private Boolean isTopped;

    /**
     * 最近活跃时间，当前未使用
     */
    @JsonProperty("latest_time")
    private Date latestTime;

    /**
     * 当前帖子的置顶评论ID(一级评论)
     */
    @JsonProperty("top_comment_id")
    private Integer topCommentId;

    /**
     * 当前帖子是否可编辑
     */
    private Boolean editable;

    /**
     * 帖子创建者
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User creator;

    /**
     * 当前用户对该帖子的权限
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PostRight rights;

    /**
     * 预览的一级评论列表，在查看某个帖子详情的时候会用到，用于数据传输
     */
    @JsonProperty("preview_comment_list")
    private List<CommentL1> previewCommentList;

    /**
     * 该帖子是否在最新帖子索引中
     */
    @JsonIgnore
    private Boolean lIndexedFlag;

    /**
     * 该帖子是否在最热帖子索引中
     */
    @JsonIgnore
    private Boolean pIndexedFlag;

    public Post(Integer id) {
        this.id = id;
    }

    @JsonProperty("type_str")
    public String getTypeStr() {
        if (Post.TYPE_PUBLIC.equals(type)) {
            return "public";
        }
        if (Post.TYPE_FORWARD.equals(type)) {
            return "forward";
        }
        if (Post.TYPE_PRIVATE.equals(type)) {
            return "private";
        }
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

    @Override
    @JsonProperty("parent_id")
    public Integer getParentId() {
        if (ofType == null || ofType == OF_CATEGORY) {
            return getCategoryId();
        }
        if (ofType == OF_CREATOR) {
            return getCreatorId();
        }
        if (ofType == OF_SUP_POST) {
            return getSupId();
        }
        return null;
    }

    @JsonProperty("parent_id")
    public Post setParentId(Integer id) {
        if (ofType == null || ofType == OF_CATEGORY) {
            return setCategoryId(id);
        }
        if (ofType == OF_CREATOR) {
            return setCreatorId(id);
        }
        if (ofType == OF_SUP_POST) {
            return setSupId(id);
        }
        return this;
    }

    @Override
    @JsonIgnore
    public AbstractContent<Post> getParent() {
        return null;
    }

    @Override
    @JsonIgnore
    public Long getChildNum() {
        return getCommentCnt();
    }

    @Override
    @JsonProperty("creator_id")
    public Integer getCreatorId() {
        return creator == null ? null : creator.getId();
    }

    @JsonProperty("category_id")
    public Post setCategoryId(Integer id) {
        if (category == null) {
            category = new Category();
        }
        category.setId(id);
        return this;
    }

    @JsonProperty("creator_id")
    public Post setCreatorId(Integer id) {
        if (creator == null) {
            creator = new User();
        }
        creator.setId(id);
        return this;
    }

    @Override
    @JsonIgnore
    public Post setEntityContent(@NotNull Post post) {
        this.title = post.title;
        this.content = post.content;
        this.rawImg = post.rawImg;
        return this;
    }

    @JsonIgnore
    public boolean validatePostContent() {
        if (title != null && title.length() > 40) {
            return false;
        }
        if (content == null || content.length() < 10 || content.length() > 100000) {
            return false;
        }
        // 允许手动设置全文
        if (fullText != null && (fullText.length() < 10 || fullText.length() > 100000)) {
            return false;
        }
        if (rawImg != null && rawImg.length() > 256) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public boolean isValidInsertParam() {
        if (type == null) {
            return false;
        }
        if (type.equals(TYPE_PUBLIC) && (category == null || category.getId() == null)) {
            return false;
        }
        return validatePostContent();
    }

    @JsonIgnore
    public boolean isValidInsertForwardParam() {
        if (content == null || supId == null) {
            return false;
        }
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
    public boolean isValidSetCategoryParam() {
        return id != null && getCategoryId() != null;
    }

    @JsonIgnore
    public boolean isValidUpdateContentParam() {
        return id != null && validatePostContent();
    }

    @JsonIgnore
    public boolean isValidCancelTopCommentParam() {
        return id != null;
    }

    @JsonIgnore
    public boolean isValidReadIndexOfCategoryParam(boolean format, Operator operator) {
        if (format) {
            this.setSupId(null).setOriId(null).setAllowForward(null).setType(null).setState(null);
            this.creator = null;
        } else {
            if (supId != null || oriId != null || allowForward != null || type != null || state != null || creator != null) {
                return false;
            }
        }
        if (category == null || category.getId() == null) {
            return false;
        }
        // 用户查询时一次最多查10个，程序本身查询，最多查1000个
        int maxQueryNum = ProgOperatorManager.isProgOperator(operator) ? 1000 : 10;
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > maxQueryNum) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public boolean isValidReadIndexOfCreatorParam(boolean format) {
        if (format) {
            this.setSupId(null).setOriId(null).setAllowForward(null).setType(null).setState(null);
            this.category = null;
        } else {
            if (supId != null || oriId != null || allowForward != null || type != null || state != null || category != null) {
                return false;
            }
        }
        if (creator == null || creator.getId() == null) {
            return false;
        }
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public boolean isValidReadForwardListParam() {
        if (supId == null) {
            return false;
        }
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10) {
            return false;
        }
        return true;
    }

}
