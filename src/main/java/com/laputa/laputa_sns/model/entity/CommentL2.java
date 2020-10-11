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

/**
 * 二级评论
 * @author JQH
 * @since 下午 12:14 20/02/29
 */

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@JsonFilter("CommentL2Filter")
public class CommentL2 extends AbstractContent<CommentL2> {

    private String entityType = "CML2";

    private String content;
    @JsonProperty("raw_img")
    private String rawImg;
    @JsonProperty("like_cnt")
    private Long likeCnt;
    @JsonProperty("post_id")
    private Integer postId;
    @JsonProperty("reply_to_l2_id")
    private Integer replyToL2Id;

    @JsonProperty("liked_by_viewer")
    private Boolean likedByViewer;

    @JsonIgnore
    private CommentL1 l1;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User creator;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User replyToUser;

    @JsonIgnore
    private Integer categoryId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CommentRight rights;

    @JsonIgnore
    private Boolean pIndexedFlag;

    public CommentL2(Integer id) {
        this.id = id;
    }

    @JsonProperty("creator_id")
    public Integer getCreatorId() {
        return creator == null ? null : creator.getId();
    }

    @JsonProperty("creator_id")
    public CommentL2 setCreatorId(Integer id) {
        if (creator == null)
            creator = new User();
        creator.setId(id);
        return this;
    }

    @JsonProperty("reply_to_user_id")
    public Integer getReplyToUserId() {
        return replyToUser == null ? null : replyToUser.getId();
    }

    @JsonProperty("reply_to_user_id")
    public CommentL2 setReplyToUserId(Integer id) {
        if (replyToUser == null)
            replyToUser = new User();
        replyToUser.setId(id);
        return this;
    }

    @JsonProperty("l1_id")
    public Integer getL1Id() {
        return l1 == null ? null : l1.getId();
    }

    @JsonProperty("l1_id")
    public CommentL2 setL1Id(Integer id) {
        if (l1 == null)
            l1 = new CommentL1();
        l1.setId(id);
        return this;
    }

    @JsonProperty("parent_id")
    public CommentL2 setParentId(Integer id) {
        return setL1Id(id);
    }

    @JsonProperty("parent_id")
    public Integer getParentId() {
        return getL1Id();
    }

    @JsonIgnore
    public CommentL1 getParent() {
        return getL1();
    }

    @JsonIgnore
    public Long getChildNum() {
        return 0L;
    }

    public CommentL2 setIsTopped(Boolean isTopped){return this;}

    @JsonIgnore
    public CommentL2 setEntityContent(@NotNull CommentL2 comment) {
        this.rawImg = comment.rawImg;
        this.content = comment.content;
        return this;
    }

    @JsonIgnore
    public boolean isValidReadIndexParam(int previewCml2Num) {
        if (postId != null || creator != null)//避免污染选择条件
            return false;
        if (l1 == null || l1.getId() == null)
            return false;
        if (queryParam == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10
                || queryParam.getQueryNum() < previewCml2Num)
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
        return l1 != null && l1.getId() != null && validateContent();
    }

    @JsonIgnore
    public boolean isValidDeleteParam() {
        return id != null;
    }

}
