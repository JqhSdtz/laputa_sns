package com.laputa.laputa_sns.validator;

import com.laputa.laputa_sns.model.entity.Category;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.right.CategoryRight;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.service.PermissionService;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import com.laputa.laputa_sns.vo.CategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.laputa.laputa_sns.validator.AdminLevel.*;

/**
 * @author JQH
 * @since 下午 3:55 20/02/15
 */

@Slf4j
@Component
public class CategoryValidator {

    @Value("${debug}")
    private boolean isDebug;

    private final PermissionService permissionService;

    public CategoryValidator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setRights(CategoryVo categoryVo, @NotNull Operator operator) {
        if (operator.getUserId().equals(-1))
            return;
        Category category = categoryVo.getRoot();
        categoryVo.setRights(new CategoryRight());
        Integer categoryId = categoryVo.getRoot().getId();
        Integer parentId = categoryVo.getRoot().getParentId();
        Integer permissionLevel = permissionService.readPermissionLevel(categoryId, operator).getObject();
        Integer parentPermLevel = permissionService.readPermissionLevel(parentId, operator).getObject();
        if (permissionLevel == null)
            return;
        CategoryRight rights = categoryVo.getRights();
        rights.setThisLevel(permissionLevel);
        rights.setParentLevel(parentPermLevel);
        rights.setUpdateInfo(permissionLevel >= UPDATE_CATEGORY_INFO);
        rights.setUpdatePreCachedNum(permissionLevel >= UPDATE_CATEGORY_PRE_CACHED_NUM);
        rights.setTopPost(permissionLevel >= SET_CATEGORY_TOP_POST);
        if (category.getAllowPostLevel() == null) {
            rights.setUpdateAllowPostLevel(permissionLevel >= SET_ALLOW_POST_LEVEL);
        } else {
            rights.setUpdateAllowPostLevel(permissionLevel > category.getAllowPostLevel());
        }
        rights.setCreateEditablePost(permissionLevel >= CREATE_EDITABLE_POST);
        rights.setDefSub(permissionLevel >= SET_CATEGORY_DEF_SUB);
        rights.setUpdateParent(permissionLevel >= UPDATE_CATEGORY_PARENT);
        rights.setDelete(permissionLevel >= DELETE_CATEGORY);
        if (categoryId == CategoryService.GROUND_ID) {
            Integer groundPermLevel = operator.getPermissionMap().get(CategoryService.GROUND_ID);
            if (groundPermLevel >= TALK_BAN)
                rights.setTalkBan(true);
        }
        if (parentPermLevel == null)
            return;
        rights.setCreate(parentPermLevel >= CREATE_CATEGORY);
        rights.setUpdateDispSeq(parentPermLevel >= UPDATE_CATEGORY_DISP_SEQ);
    }

    /**
     * 检查更新目录信息权限
     */
    public boolean checkUpdateInfoPermission(Category category, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if (operator.getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= UPDATE_CATEGORY_INFO;
    }

    public boolean checkUpdateCacheNumPermission(Category category, @NotNull Operator operator) {
        if (operator.getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= UPDATE_CATEGORY_PRE_CACHED_NUM;
    }

    /**
     * 检查更新目录置顶帖子权限
     */
    public boolean checkUpdateTopPostPermission(Category category, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if (operator.getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= SET_CATEGORY_TOP_POST;
    }

    /**
     * 检查更新目录允许发帖管理等级权限
     */
    public boolean checkUpdateAllowPostLevelPermission(Category origin, Category category, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if (operator.getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        if (origin.getAllowPostLevel() != null) {
            // 若已经设置该等级，则修改人等级需大于已设置等级
            return permissionLevel != null && permissionLevel > origin.getAllowPostLevel();
        } else {
            // 否则应大于等于设定的管理等级
            return permissionLevel != null && permissionLevel >= SET_ALLOW_POST_LEVEL;
        }
    }

    /**
     * 检查更新目录置顶帖子权限
     */
    public boolean checkUpdateDefSubPermission(Category category, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if (operator.getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= SET_CATEGORY_DEF_SUB;
    }

    public boolean checkCreatePermission(@NotNull Category category, @NotNull Operator operator) {//创建目录操作要求父目录权限
        if (operator.getId().equals(-1))
            return false;
        if (ProgOperatorManager.isProgOperatorOfClass(operator, CategoryService.class))
            return true;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getParentId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= CREATE_CATEGORY;
    }

    public boolean checkUpdateDispSeqPermission(Category category, @NotNull Operator operator) {
        if (operator.getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getParentId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= UPDATE_CATEGORY_DISP_SEQ;
    }

    /**
     * 检查更新目录父目录权限
     */
    public boolean checkUpdateParentPermission(Category category, Integer newParentId, @NotNull Operator operator) {//更新目录操作要求本目录权限
        if (isDebug) 
            return true;
        if (operator.getId().equals(-1))
            return false;
        Integer permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        Integer newParentPermLevel = permissionService.readPermissionLevel(newParentId, operator).getObject();
        return permissionLevel != null && permissionLevel >= UPDATE_CATEGORY_PARENT && newParentPermLevel != null
                && newParentPermLevel >= CREATE_CATEGORY;
    }

    public boolean checkDeletePermission(Category category, @NotNull Operator operator, boolean forcibly) {//删除目录操作要求父目录权限
        if (operator.getId().equals(-1))
            return false;
        Integer permissionLevel;
        if (forcibly)//强制删除需要父目录权限
            permissionLevel = permissionService.readPermissionLevel(category.getParentId(), operator).getObject();
        else//非强制删除仅需本目录权限
            permissionLevel = permissionService.readPermissionLevel(category.getId(), operator).getObject();
        return permissionLevel != null && permissionLevel >= DELETE_CATEGORY;
    }

}
