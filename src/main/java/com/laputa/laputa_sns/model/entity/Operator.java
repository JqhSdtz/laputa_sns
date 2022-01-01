package com.laputa.laputa_sns.model.entity;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.validator.AdminLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 操作者，即登录用户
 * @author JQH
 * @since 上午 9:48 20/02/19
 */

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
@JsonFilter("OperatorFilter")
public class Operator extends AbstractBaseEntity {

    private String entityType = "OPERATOR";

    /**
     * 用户基本信息
     */
    private User user;

    /**
     * 用户权限信息，Key是categoryId
     */
    @JsonProperty("permission_map")
    private Map<Integer, Integer> permissionMap;

    /**
     * 用户关注列表
     */
    @JsonProperty("follow_list")
    private List<Follow> followList;

    /**
     * 用户的token，和User类中的token相同，用于登录验证
     */
    private String token;

    /**
     * 登录信息，目前是登录失败的时候设置并返回
     */
    @JsonProperty("log_message")
    private String logMessage;

    /**
     * 登录过期时间，该字段当前未使用
     */
    @JsonProperty("expire_seconds")
    private Integer expireSeconds = 1800;

    /**
     * 用户当前未读动态数量
     */
    @JsonProperty("unread_news_cnt")
    private Integer unreadNewsCnt;

    /**
     * 用户当前未读消息数量
     */
    @JsonProperty("unread_notice_cnt")
    private Long unreadNoticeCnt;

    /**
     * 用户上一次发送请求的时间
     */
    @JsonProperty("last_access_time")
    private Long lastAccessTime;

    /**
     * 该对象是否处于一个登录请求中
     */
    @JsonProperty("from_login")
    private Boolean fromLogin;

    @JsonProperty("from_register")
    private Boolean fromRegister;

    public Operator(Integer id) {
        user = new User(id);
    }

    @JsonProperty("user_id")
    public Integer getUserId() {
        return user.getId();
    }

    @JsonProperty("user_id")
    public Operator setUserId(Integer id) {
        if(user == null) {
            user = new User(id);
        } else {
            user.setId(id);
        }
        return this;
    }

    @Override
    @JsonIgnore
    public Integer getId() {
        return getUserId();
    }

    @Override
    public Operator setId(Integer id) {
        return setUserId(id);
    }

    @JsonIgnore
    public Operator setUserName(String name) {
        user.setNickName(name);
        return this;
    }

    @JsonIgnore
    public boolean isSuperAdmin() {
        Integer groundLevel = permissionMap == null ? null : permissionMap.get(CategoryService.GROUND_ID);
        return groundLevel != null && groundLevel == AdminLevel.SUPER_ADMIN;
    }

    @JsonIgnore
    public boolean isAdmin() {
        return getUserId() != -1 && permissionMap != null && permissionMap.size() != 0;
    }
}
