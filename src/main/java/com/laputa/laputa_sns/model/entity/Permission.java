package com.laputa.laputa_sns.model.entity;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 权限，
 * @author JQH
 * @since 下午 4:31 20/02/05
 */

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonFilter("PermissionFilter")
public class Permission extends AbstractBaseEntity {

    /**
     * 该权限对象从属于某个目录下，即处于查询某个目录的有权限的用户列表的请求中
     */
    public static final int OF_CATEGORY = 1;
    /**
     * 该权限对象从属于某个用户下，即处于查询某个用户的有权限的目录列表的请求中
     */
    public static final int OF_USER = 2;

    private Integer ofType;

    private User user;

    @JsonIgnore
    private Category category;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User creator;

    /**
     * 权限等级，在执行某些需要权限的操作时进行判断
     */
    private Integer level;

    @JsonProperty("category_path")
    public List<Category> getCategoryPath() {
        return category == null ? null : category.getPathList();
    }

    @JsonProperty("category_id")
    public Integer getCategoryId() {
        return category == null ? null : category.getId();
    }

    @JsonProperty("category_id")
    public Permission setCategoryId(Integer id) {
        if (category == null)
            category = new Category();
        category.setId(id);
        return this;
    }

    @JsonProperty("user_id")
    public Integer getUserId() {
        return user == null ? null : user.getId();
    }

    @JsonProperty("user_id")
    public Permission setUserId(Integer id) {
        if (user == null)
            user = new User();
        user.setId(id);
        return this;
    }

    private boolean hasBasic() {
        return user != null && user.getId() != null && category != null && category.getId() != null;
    }

    @JsonIgnore
    /**用于在Permission对象作为参数对象的时候判断是否合法*/
    public boolean isValidUpdateParam() {
        return hasBasic() && level != 0;//更新权限不能为0
    }

    @JsonIgnore
    /**用于在Permission对象作为新添加对象的时候判断是否合法*/
    public boolean isValidInsertParam() {//新添加的权限不能为0
        return hasBasic() && level != null && level != 0;
    }

    @JsonIgnore
    /**用于在Permission对象作为新删除对象的时候判断是否合法*/
    public boolean isValidDeleteParam() {//删除对象必须指定userId和categoryId
        return hasBasic();
    }

}
