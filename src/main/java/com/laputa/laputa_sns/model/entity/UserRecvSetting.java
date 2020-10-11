package com.laputa.laputa_sns.model.entity;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 用户的消息接收设置，当前未使用
 * @author JQH
 * @since 上午 11:41 20/04/15
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonFilter("UserRecvSettingFilter")
public class UserRecvSetting  extends AbstractBaseEntity {
    private User user;
    /**
     * 是否接收点赞消息
     */
    @JsonProperty("recv_like")
    private Boolean recvLike;
    /**
     * 是否接收电子邮件
     */
    @JsonProperty("recv_email")
    private Boolean recvEmail;
    /**
     * 是否接收微信
     */
    @JsonProperty("recv_wx")
    private Boolean recvWx;

    public UserRecvSetting(User user) {
        this.user = user;
    }

    public UserRecvSetting(Integer id) {
        this.user = new User(id);
    }

    public Integer getId() {
        return user == null ? null : user.getId();
    }

    @JsonIgnore
    public boolean isValidUpdateParam() {
        if (user == null || user.getId() == null)
            return false;
        if (recvLike == null && recvEmail == null && recvWx == null)
            return false;
        return true;
    }
}
