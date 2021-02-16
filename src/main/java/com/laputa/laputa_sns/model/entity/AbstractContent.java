package com.laputa.laputa_sns.model.entity;

import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Post,CommentL1,CommentL2的父类
 * @author JQH
 * @since 下午 4:11 20/03/20
 */

@Getter
@Setter
public abstract class AbstractContent<T> extends AbstractBaseEntity {
    abstract public T setLikeCnt(Long value);
    abstract public Long getLikeCnt();
    abstract public T setLikedByViewer(Boolean likedByViewer);
    abstract public Boolean getLikedByViewer();
    abstract public Integer getParentId();
    abstract public AbstractContent getParent();
    abstract public T setEntityContent(T content);
    abstract public Long getChildNum();
    abstract public Integer getCreatorId();
    abstract public T setCreator(User creator);
    abstract public User getCreator();
    abstract public Boolean getIsTopped();
    abstract public T setIsTopped(Boolean isTopped);
    public Integer getOfId() {
        return getParentId();
    }
}
