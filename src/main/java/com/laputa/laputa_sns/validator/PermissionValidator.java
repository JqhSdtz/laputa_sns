package com.laputa.laputa_sns.validator;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Category;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Permission;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.service.PermissionService;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author JQH
 * @since 下午 4:23 20/02/15
 */

@Component
public class PermissionValidator {

    private final CategoryService categoryService;
    private final PermissionService permissionService;

    public PermissionValidator(CategoryService categoryService, PermissionService permissionService) {
        this.categoryService = categoryService;
        this.permissionService = permissionService;
    }

    @Nullable
    private Integer getParentCategoryId(Integer categoryId, Operator operator) {
        Result<Category> result = categoryService.readCategory(categoryId, false, operator);
        if (result.getState() == Result.FAIL) {
            return null;
        }
        return result.getObject().getParentId();
    }

    public boolean checkCreatePermission(@NotNull Permission permission, @NotNull Operator operator) {
        if (!operator.isAdmin()) {
            return false;
        }
        if (operator.isSuperAdmin()) {
            return true;
        }
        // 创建权限操作要求父目录权限
        Integer parentId = getParentCategoryId(permission.getCategoryId(), operator);
        Integer operatorLevel = permissionService.readPermissionLevel(parentId, operator).getObject();
        return operatorLevel != null && operatorLevel >= permission.getLevel();
    }

    public boolean checkUpdatePermission(@NotNull Permission oldPerm, Permission newPerm, @NotNull Operator operator) {
        if (!operator.isAdmin()) {
            return false;
        }
        if (operator.isSuperAdmin()) {
            return true;
        }
        // 更新权限操作要求父目录权限
        Integer parentId = getParentCategoryId(newPerm.getCategoryId(), operator);
        Integer operatorLevel = permissionService.readPermissionLevel(parentId, operator).getObject();
        if (operatorLevel == null || oldPerm.getLevel() == null) {
            return false;
        }
        //不能给比操作者等级高的用户改等级，不能给用户设置高于操作者等级的等级
        if (operatorLevel < oldPerm.getLevel() || operatorLevel < newPerm.getLevel()) {
            return false;
        }
        //不能给和操作者同级的用户设置更低的等级
        if (operatorLevel.equals(oldPerm.getLevel()) && newPerm.getLevel() < oldPerm.getLevel()) {
            return false;
        }
        return true;
    }

    public boolean checkDeletePermission(Permission permission, @NotNull Operator operator) {
        if (!operator.isAdmin()) {
            return false;
        }
        if (operator.isSuperAdmin()) {
            return true;
        }
        // 删除权限操作要求父目录权限
        Integer parentId = getParentCategoryId(permission.getCategoryId(), operator);
        // 父目录权限等级
        Integer operatorLevel = permissionService.readPermissionLevel(parentId, operator).getObject();
        // 操作者的父目录权限等级大于要删除的权限在指定目录的等级
        return operatorLevel != null && operatorLevel > permission.getLevel();
    }

    public boolean checkReadPermission(@NotNull Permission permission, Operator operator) {
        return true;
    }

    public boolean checkReadOfUserPermission(Operator operator) {
        //检查读取用户权限列表的权限
        return operator.isAdmin() || ProgOperatorManager.isProgOperator(operator);
    }

}
