package com.laputa.laputa_sns.model.entity;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.right.CommentRight;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

/**
 * 一级评论
 * @author JQH
 * @since 下午 4:31 20/02/26
 */

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@JsonFilter("CommentL1Filter")
public class CommentL1 extends AbstractContent<CommentL1> {

    private String entityType = "CML1";

    /**
     * 评论内容
     */
    private String content;
    /**
     * 评论图片的原始字段值，即多个路径拼成的字符串
     */
    @JsonProperty("raw_img")
    private String rawImg;
    /**
     * 二级评论的数量
     */
    @JsonProperty("l2_cnt")
    private Long l2Cnt;
    /**
     * 点赞的数量
     */
    @JsonProperty("like_cnt")
    private Long likeCnt;
    /**
     * 评论对应帖子的发帖人在此评论下发表的二级评论的数量
     */
    @JsonProperty("poster_rep_cnt")
    private Long posterRepCnt;
    /**
     * 最近回复时间，当前未使用
     */
    private Date latestReplyTime;

    /**
     * 该评论是否被当前登录用户点赞
     */
    @JsonProperty("liked_by_viewer")
    private Boolean likedByViewer;

    /**
     * 该评论是否在所处的帖子中被置顶
     */
    @JsonProperty("is_topped")
    private Boolean isTopped;

    /**
     * 该评论所对应的帖子
     */
    @JsonIgnore
    private Post post;

    /**
     * 该评论所对应帖子所处的目录
     */
    @JsonIgnore
    private Integer categoryId;

    /**
     * 评论创建者
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User creator;

    /**
     * 当前用户对该评论的权限
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CommentRight rights;

    /**
     * 预览的二级评论列表，查看某个帖子下的评论时，一级评论要附带上指定数量的二级评论列表
     */
    @JsonProperty("preview_l2_list")
    private List<CommentL2> previewL2List;

    /**
     * 该评论是否在帖子的最新评论索引中
     */
    @JsonIgnore
    private Boolean lIndexedFlag;

    /**
     * 该评论是否在帖子的最热评论索引中
     */
    @JsonIgnore
    private Boolean pIndexedFlag;

    public CommentL1(Integer id) {
        this.id = id;
    }

    @JsonProperty("creator_id")
    public Integer getCreatorId() {
        return creator == null ? null : creator.getId();
    }

    @JsonProperty("creator_id")
    public CommentL1 setCreatorId(Integer id) {
        if (creator == null)
            creator = new User();
        creator.setId(id);
        return this;
    }

    @JsonProperty("post_id")
    public Integer getPostId() {
        return post == null ? null : post.getId();
    }

    @JsonProperty("post_id")
    public CommentL1 setPostId(Integer id) {
        if (post == null)
            post = new Post();
        post.setId(id);
        return this;
    }

    @JsonIgnore
    public CommentL1 setEntityContent(@NotNull CommentL1 comment) {
        this.rawImg = comment.rawImg;
        this.content = comment.content;
        return this;
    }

    @JsonProperty("parent_id")
    public CommentL1 setParentId(Integer id) {
        return setPostId(id);
    }

    @JsonProperty("parent_id")
    public Integer getParentId() {
        return getPostId();
    }

    @JsonIgnore
    public Post getParent() {
        return getPost();
    }

    @JsonIgnore
    public Long getChildNum() {
        return getL2Cnt();
    }

    @JsonIgnore
    public boolean isValidReadIndexParam() {
        if (creator != null)//避免污染选择条件
            return false;
        if (post == null || post.getId() == null)
            return false;
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean validateContent() {
        if (content == null || content.length() == 0 || content.length() > 256)
            return false;
        if (rawImg != null && rawImg.length() > 256)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidInsertParam() {
        return post != null && post.getId() != null && validateContent();
    }

    @JsonIgnore
    public boolean isValidDeleteParam() {
        return id != null;
    }

}
