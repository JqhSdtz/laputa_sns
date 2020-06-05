package com.laputa.laputa_sns.validator;

import com.laputa.laputa_sns.model.Category;
import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.right.CategoryRight;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.service.PermissionService;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import com.laputa.laputa_sns.vo.CategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import static com.laputa.laputa_sns.validator.AdminLevel.*;

/**
 * @author JQH
 * @since 下午 3:55 20/02/15
 */

@Slf4j
@Component
public class CategoryValidator {

    private final PermissionService permissionService;

    public CategoryValidator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setRights(CategoryVo categoryVo, @NotNull Operator operator) {
        if (operator.getUserId().equals(-1))
            return;
        categoryVo.setRights(new CategoryRight());
        Integer permissionLevel = permissionService.readPermissionLevel(categoryVo.getRoot().getId(), operator).getObject();
        Integer parentPermLevel = permissionService.readPermissionLevel(categoryVo.getRoot().getParentId(), operator).getObject();
        if (permissionLevel == null)
            return;
        CategoryRight rights = categoryVo.getRights();
        rights.setThisLevel(permissionLevel);
        rights.setParentLevel(parentPermLevel);
        if (permissionLevel >= UPDATE_CATEGORY_INFO)
            rights.setUpdateInfo(true);
        if (permissionLevel >= UPDATE_CATEGORY_PRE_CACHED_NUM)
            rights.setUpdatePreCachedNum(true);
        if (permissionLevel >= SET_CATEGORY_TOP_POST)
            rights.setTopPost(true);
        if (permissionLevel >= SET_CATEGORY_DEF_SUB)
            rights.setDefSub(true);
        if (permissionLevel >= UPDATE_CATEGORY_PARENT)
            rights.setUpdateParent(true);
        if (permissionLevel >= DELETE_CATEGORY)
            rights.setDelete(true);
        if (parentPermLevel == null)
            return;
        if (parentPermLevel >= CREATE_CATEGORY)
            rights.setCreate(true);
        if (parentPermLevel >= UPDATE_CATEGORY_DISP_SEQ)
            rights.setUpdateDispSeq(true);
    }

    /**检查更新目录信息权限*/
    public boolean checkUpdateInfoPermission(Category category, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if(operator .getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= UPDATE_CATEGORY_INFO;
    }

    public boolean checkUpdateCacheNumPermission(Category category, @NotNull Operator operator) {
        if(operator .getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= UPDATE_CATEGORY_PRE_CACHED_NUM;
    }

    /**检查更新目录置顶贴子权限*/
    public boolean checkUpdateTopPostPermission(Category category, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if(operator .getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= SET_CATEGORY_TOP_POST;
    }

    /**检查更新目录置顶贴子权限*/
    public boolean checkUpdateDefSubPermission(Category category, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if(operator .getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= SET_CATEGORY_DEF_SUB;
    }

    public boolean checkCreatePermission(@NotNull Category category, @NotNull Operator operator) {//创建目录操作要求父目录权限
        if(operator .getId().equals(-1))
            return false;
        if(ProgOperatorManager.isProgOperatorOfClass(operator, CategoryService.class))
            return true;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getParentId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= CREATE_CATEGORY;
    }

    public boolean checkUpdateDispSeqPermission(Category category, @NotNull Operator operator) {
        if(operator .getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getParentId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= UPDATE_CATEGORY_DISP_SEQ;
    }

    /**检查更新目录父目录权限*/
    public boolean checkUpdateParentPermission(Category category, Integer newParentId, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if(operator .getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        Integer newParentPermLevel = permissionService.readPermissionLevel(newParentId, operator).getObject();
        return permissionLevel != null && permissionLevel >= UPDATE_CATEGORY_PARENT && newParentPermLevel != null
                && newParentPermLevel >= CREATE_CATEGORY;
    }

    public boolean checkDeletePermission(Category category, @NotNull Operator operator, boolean forcibly) {//删除目录操作要求父目录权限
        if(operator .getId().equals(-1))
            return false;
        Integer permissionLevel;
        if(forcibly)//强制删除需要父目录权限
            permissionLevel = permissionService.readPermissionLevel(category.getParentId(), operator).getObject();
        else//非强制删除仅需本目录权限
            permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= DELETE_CATEGORY;
    }

}
