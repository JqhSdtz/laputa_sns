package com.laputa.laputa_sns.validator;

import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.right.PostRight;
import com.laputa.laputa_sns.service.PermissionService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author JQH
 * @since 下午 2:06 20/02/23
 */

@Component
public class PostValidator {

    private final PermissionService permissionService;

    public PostValidator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setRights(@NotNull Post post, @NotNull Operator operator, Integer level) {
        post.setRights(new PostRight());
        if (operator.getUserId().equals(-1))
            return;
        if (post.getCreatorId().equals(operator.getUserId()))
            post.getRights().setTop(true);
        //非公开的帖子删除需要根目录权限
        if (level == null)
            level = permissionService.readPermissionLevel(post.getCategoryIdForRight(), operator).getObject();
        if (level == null)
            return;
        if (level >= AdminLevel.DELETE_CATEGORY_POST)
            post.getRights().setDelete(true);
    }

    public void multiSetRights(Post param, @NotNull List<Post> postList, @NotNull Operator operator) {
        if (operator.getUserId().equals(-1))
            return;
        Integer level = null;
        Boolean beTopped = null;
        if (param != null && param.getOfType() != null) {
            if (param.getOfType().equals(Post.OF_CATEGORY)) {
                level = permissionService.readPermissionLevel(param.getCategoryId(), operator).getObject();
                level = level == null ? 0 : level;
                beTopped = level == null ? null : level >= AdminLevel.SET_CATEGORY_TOP_POST;
            } else if (param.getOfType().equals(Post.OF_CREATOR))
                beTopped = param.getCreatorId().equals(operator.getUserId());
        }
        for (int i = 0; i < postList.size(); ++i)
            if (postList.get(i) != null) {
                setRights(postList.get(i), operator, level);
                postList.get(i).getRights().setBeTopped(beTopped);
            }
    }

    public boolean checkCreatePermission(Post post, @NotNull Operator operator) {
        if (operator.getId().equals(-1))
            return false;
        Date talkBanTo = operator.getUser().getTalkBanTo();
        if (talkBanTo != null && talkBanTo.before(new Date()))
            return false;
        return true;
    }

    public boolean checkCreateForwardPermission(Post sup, @NotNull Operator operator) {
        if (operator .getId().equals(-1))
            return false;
        if (sup.getAllowForward().equals(0) && !sup.getCreatorId().equals(operator.getUserId()))
            return false;
        Date talkBanTo = operator.getUser().getTalkBanTo();
        if (talkBanTo != null && talkBanTo.before(new Date()))
            return false;
        return true;
    }

    public boolean checkDeletePermission(@NotNull Post post, @NotNull Operator operator) {
        if (operator .getId().equals(-1))
            return false;
        if (post.getCreatorId().equals(operator.getUserId()))
            return true;
        if (post.getType().equals(Post.TYPE_PUBLIC) && operator.isAdmin()) {
            Integer permissionLevel = permissionService.readPermissionLevel(post.getCategoryId(), operator).getObject();
            return permissionLevel != null && permissionLevel >= AdminLevel.DELETE_CATEGORY_POST;
        }
        return false;
    }

    public boolean checkSetTopCommentPermission(@NotNull Post post, @NotNull Operator operator) {
        if (operator .getId().equals(-1))
            return false;
        return post.getCreatorId().equals(operator.getUserId());
    }

    public boolean checkUpdateContentPermission(@NotNull Post post, @NotNull Operator operator) {
        if (operator .getId().equals(-1))
            return false;
        return post.getCreatorId().equals(operator.getUserId());
    }

}
