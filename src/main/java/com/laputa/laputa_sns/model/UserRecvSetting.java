package com.laputa.laputa_sns.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
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
    @JsonProperty("recv_like")
    private Boolean recvLike;
    @JsonProperty("recv_email")
    private Boolean recvEmail;
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
