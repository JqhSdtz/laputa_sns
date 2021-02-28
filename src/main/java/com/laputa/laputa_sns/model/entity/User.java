package com.laputa.laputa_sns.model.entity;

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
 * 用户
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

    /**
     * 昵称，也是登录名，可以是汉字
     */
    @JsonProperty("nick_name")
    private String nickName;
    /**
     * 登录密码
     */
    @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * 密码的salt
     */
    @JsonIgnore
    private String pwdSalt;
    /**
     * 用户的登录token
     */
    @JsonIgnore
    private String token;

    @JsonProperty("wx_unionid")
    private String wxUnionId;
    private String email;
    /**
     * 用户的学校信息
     */
    @JsonProperty("sch_info")
    private String schInfo;
    private String phone;
    /**
     * 用户的个人介绍
     */
    private String intro;
    /**
     * 用户的头像地址
     */
    @JsonProperty("raw_avatar")
    private String rawAvatar;

    /**
     * 用户个人中心的置顶帖子
     */
    @JsonProperty("top_post_id")
    private Integer topPostId;

    /**
     * 粉丝数量
     */
    private Long followersCnt;
    /**
     * 关注数量
     */
    private Long followingCnt;
    /**
     * 帖子数量
     */
    private Long postCnt;

    /**
     * 当前登录用户是否关注了该用户
     */
    @JsonProperty("followed_by_viewer")
    private Boolean followedByViewer;

    /**
     * 最近一次获取动态的时间
     */
    @JsonIgnore
    private Date lastGetNewsTime;
    /**
     * 最近一次登录的时间
     */
    @JsonProperty("last_login_time")
    private Date lastLoginTime;
    /**
     * 最近一次修改昵称的时间
     */
    @JsonProperty("last_alter_name_time")
    private Date lastAlterNameTime;
    /**
     * 被禁言的截止时间
     */
    @JsonProperty("talk_ban_to")
    private Date talkBanTo;

    /**
     * 最近访问的目录，用';'连接的目录ID字符串
     */
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

    @JsonProperty("post_cnt")
    public Long getPostCnt() {
        return postCnt == null ? 0 :postCnt;
    }

    @JsonProperty("followers_cnt")
    public Long getFollowersCnt() {
        return followersCnt == null ? 0 : followersCnt;
    }

    @JsonProperty("following_cnt")
    public Long getFollowingCnt() {
        return followingCnt == null ? 0 : followingCnt;
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
    public boolean isValidUpdateInfoParam(boolean format) {
        if (id == null)
            return false;
        if (format) {
            this.setNickName(null).setFollowersCnt(null).setFollowingCnt(null).setPostCnt(null);
        } else {
            if (nickName != null || followersCnt != null || followingCnt != null || postCnt != null)
                return false;//这些需特殊判断
        }
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
