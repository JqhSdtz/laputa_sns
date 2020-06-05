package com.laputa.laputa_sns.model;

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

    private String content;
    @JsonProperty("raw_img")
    private String rawImg;
    @JsonProperty("l2_cnt")
    private Long l2Cnt;
    @JsonProperty("like_cnt")
    private Long likeCnt;
    @JsonProperty("poster_rep_cnt")
    private Long posterRepCnt;
    private Date latestReplyTime;

    @JsonProperty("liked_by_viewer")
    private Boolean likedByViewer;

    @JsonProperty("is_topped")
    private Boolean isTopped;

    @JsonIgnore
    private Post post;

    @JsonIgnore
    private Integer categoryId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User creator;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CommentRight rights;

    @JsonProperty("preview_l2_list")
    private List<CommentL2> previewL2List;

    @JsonIgnore
    private Boolean lIndexedFlag;

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
