package com.laputa.laputa_sns.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * @author JQH
 * @since 上午 11:49 20/02/05
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonFilter("UserFilter")
public class User extends AbstractBaseEntity {
    //private static Pattern userNamePattern = Pattern.compile("^[^<>()\\[\\]\\\\.,;:\\s@\"]{2,12}$");
    private static char[] invalidChar = {'@', '#', '$', '.', ';', ':', '<', '>', '(', ')', '[', ']', '\\', ' ', '\0', '\n', '\t'};

    private String entityType = "USER";

    @JsonProperty("nick_name")
    private String nickName;
    @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @JsonIgnore
    private String pwdSalt;
    @JsonIgnore
    private String token;

    @JsonProperty("wx_unionid")
    private String wxUnionId;
    private String email;
    @JsonProperty("sch_info")
    private String schInfo;
    private String phone;
    private String intro;
    @JsonProperty("raw_avatar")
    private String rawAvatar;

    @JsonProperty("top_post_id")
    private Integer topPostId;

    @JsonProperty("followers_cnt")
    private Long followersCnt;
    @JsonProperty("following_cnt")
    private Long followingCnt;
    @JsonProperty("post_cnt")
    private Long postCnt;

    @JsonProperty("followed_by_viewer")
    private Boolean followedByViewer;

    @JsonIgnore
    private Date lastGetNewsTime;
    @JsonProperty("last_login_time")
    private Date lastLoginTime;
    @JsonProperty("last_alter_name_time")
    private Date lastAlterNameTime;
    @JsonProperty("talk_ban_to")
    private Date talkBanTo;

    @JsonIgnore
    private String recentVisitCategories;

    private Boolean withTokenInfo;
    private Boolean withPwdInfo;
    private Boolean fromLogin;
    private Boolean withFullInfo;
    private Boolean withProfileInfo;
    private Boolean withWxInfo;
    private Boolean withTimeInfo;

    public User(Integer id) {
        this.id = id;
    }

    @JsonIgnore
    private boolean validateNickName() {
        if (nickName == null || nickName.length() < 2 || nickName.length() > 12)
            return false;
        for (int i = 0; i < nickName.length(); ++i) {
            char ch = nickName.charAt(i);
            for (int j = 0; j < invalidChar.length; ++j)
                if (ch == invalidChar[j])
                    return false;
        }
        return true;
    }

    @JsonIgnore
    public boolean isValidUpdateInfoParam() {
        if (id == null)
            return false;
        if (nickName != null || followersCnt != null || followingCnt != null || postCnt != null)
            return false;//这些需特殊判断
        if (wxUnionId == null && email == null && schInfo == null && phone == null && intro == null && rawAvatar == null)
            return false;
        return true;
    }

    @JsonIgnore
    public boolean isValidUpdateNickNameParam() {
        return validateNickName();
    }

    @JsonIgnore
    public boolean isValidInsertParam() {
        if (!validateNickName())//验证用户名是否合法
            return false;
        if (type != null || state != null)//禁止用户手动设置type和state
            return false;
        return password != null && password.length() == 32;
    }

    public User copyUpdateParamInfo(@NotNull User param) {
        this.wxUnionId = param.wxUnionId == null ? this.wxUnionId : param.wxUnionId;
        this.email = param.email == null ? this.email : param.email;
        this.schInfo = param.schInfo == null ? this.schInfo : param.schInfo;
        this.phone = param.phone == null ? this.phone : param.phone;
        this.intro = param.intro == null ? this.intro : param.intro;
        this.rawAvatar = param.rawAvatar == null ? this.rawAvatar : param.rawAvatar;
        return this;
    }

}
