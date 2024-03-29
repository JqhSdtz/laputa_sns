package com.laputa.laputa_sns.validator;

import java.util.Date;
import java.util.List;

import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.right.PostRight;
import com.laputa.laputa_sns.service.PermissionService;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author JQH
 * @since 下午 2:06 20/02/23
 */

@Component
public class PostValidator {

    @Value("${debug}")
    private boolean isDebug;

    private final PermissionService permissionService;

    public PostValidator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setRights(@NotNull Post post, @NotNull Operator operator, Integer level) {
        post.setRights(new PostRight());
        if (operator.getUserId().equals(-1)) {
            return;
        }
        if (post.getCreatorId().equals(operator.getUserId())) {
            post.getRights().setTopComment(true).setDelete(true);
            if (post.getEditable() != null && post.getEditable()) {
                post.getRights().setEdit(true);
            }
        }
        //非公开的帖子删除需要根目录权限
        if (level == null) {
            level = permissionService.readPermissionLevel(post.getCategoryIdForRight(), operator).getObject();
        }
        if (level == null) {
            return;
        }
        if (level >= AdminLevel.DELETE_CATEGORY_POST) {
            post.getRights().setDelete(true);
        }
        if (level >= AdminLevel.UPDATE_POST_CATEGORY) {
            post.getRights().setUpdateCategory(true);
        }
    }

    public void multiSetRights(Post param, @NotNull List<Post> postList, @NotNull Operator operator) {
        if (operator.getUserId().equals(-1)) {
            return;
        }
        Integer level = null;
        Boolean beTopped = null;
        if (param != null && param.getOfType() != null) {
            if (param.getOfType().equals(Post.OF_CATEGORY)) {
                level = permissionService.readPermissionLevel(param.getCategoryId(), operator).getObject();
                level = level == null ? 0 : level;
                beTopped = level == null ? null : level >= AdminLevel.SET_CATEGORY_TOP_POST;
            } else if (param.getOfType().equals(Post.OF_CREATOR)) {
                beTopped = param.getCreatorId().equals(operator.getUserId());
            }
        }
        for (int i = 0; i < postList.size(); ++i) {
            if (postList.get(i) != null) {
                setRights(postList.get(i), operator, level);
                postList.get(i).getRights().setBeTopped(beTopped);
            }
        }
    }

    public boolean checkCreatePermission(Post post, @NotNull Operator operator) {
        if (isDebug) {
            return true;
        }
        if (operator.getId().equals(-1)) {
            return false;
        }
        Date talkBanTo = operator.getUser().getTalkBanTo();
        if (talkBanTo != null && talkBanTo.after(new Date())) {
            return false;
        }
        Integer permissionLevel = permissionService.readPermissionLevel(post.getCategoryId(), operator).getObject();
        if (post.getType().equals(Post.TYPE_PUBLIC)) {
            Integer allowPostLevel = post.getCategory().getAllowPostLevel();
            if (allowPostLevel != null && (permissionLevel == null || permissionLevel < allowPostLevel)) {
                return false;
            }
        }
        if (post.getEditable() != null && post.getEditable()) {
            if (!operator.isAdmin()) {
                return false;
            }
            return permissionLevel != null && permissionLevel >= AdminLevel.CREATE_EDITABLE_POST;
        }
        return true;
    }

    public boolean checkCreateForwardPermission(Post sup, @NotNull Operator operator) {
        if (operator .getId().equals(-1)) {
            return false;
        }
        if (sup.getAllowForward().equals(0) && !sup.getCreatorId().equals(operator.getUserId())) {
            return false;
        }
        Date talkBanTo = operator.getUser().getTalkBanTo();
        if (talkBanTo != null && talkBanTo.before(new Date())) {
            return false;
        }
        return true;
    }

    public boolean checkDeletePermission(@NotNull Post post, @NotNull Operator operator) {
        if (operator .getId().equals(-1)) {
            return false;
        }
        if (post.getCreatorId().equals(operator.getUserId())) {
            return true;
        }
        if (post.getType().equals(Post.TYPE_PUBLIC) && operator.isAdmin()) {
            Integer permissionLevel = permissionService.readPermissionLevel(post.getCategoryId(), operator).getObject();
            return permissionLevel != null && permissionLevel >= AdminLevel.DELETE_CATEGORY_POST;
        }
        return false;
    }

    public boolean checkSetTopCommentPermission(@NotNull Post post, @NotNull Operator operator) {
        if (operator .getId().equals(-1)) {
            return false;
        }
        return post.getCreatorId().equals(operator.getUserId());
    }

    public boolean checkSetCategoryPermission(@NotNull Post oriPost, @NotNull Operator operator) {
        if (operator .getId().equals(-1)) {
            return false;
        }
        if (oriPost.getCreatorId().equals(operator.getUserId())) {
            // 自己发的帖子，可以迁移
            return true;
        } else {
            // 否则，检查是否有在该目录更新贴子父目录的权限
            Integer permissionLevel = permissionService.readPermissionLevel(oriPost.getCategoryId(), operator).getObject();
            return permissionLevel != null && permissionLevel >= AdminLevel.UPDATE_POST_CATEGORY;
        }
    }

    public boolean checkUpdateContentPermission(@NotNull Post post, @NotNull Operator operator) {
        if (operator .getId().equals(-1)) {
            return false;
        }
        return post.getCreatorId().equals(operator.getUserId());
    }

}
