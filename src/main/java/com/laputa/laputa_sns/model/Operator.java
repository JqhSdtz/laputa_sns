package com.laputa.laputa_sns.model;

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

    private User user;

    @JsonProperty("permission_map")
    private Map<Integer, Integer> permissionMap;//Key是categoryId

    @JsonProperty("follow_list")
    private List<Follow> followList;

    private String token;

    @JsonProperty("log_message")
    private String logMessage;

    @JsonProperty("expire_seconds")
    private Integer expireSeconds = 1800;

    @JsonProperty("unread_news_cnt")
    private Integer unreadNewsCnt;

    @JsonProperty("unread_notice_cnt")
    private Long unreadNoticeCnt;

    @JsonProperty("last_access_time")
    private Long lastAccessTime;

    @JsonProperty("from_login")
    private Boolean fromLogin;

    public Operator(Integer id) {
        user = new User(id);
    }

    @JsonProperty("user_id")
    public Integer getUserId() {
        return user.getId();
    }

    @JsonProperty("user_id")
    public Operator setUserId(Integer id) {
        if(user == null)
            user = new User(id);
        else
            user.setId(id);
        return this;
    }

    @JsonIgnore
    public Integer getId() {
        return getUserId();
    }

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
