package com.laputa.laputa_sns.validator;

import com.laputa.laputa_sns.model.entity.CommentL2;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.right.CommentRight;
import com.laputa.laputa_sns.service.PermissionService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author JQH
 * @since 下午 1:54 20/02/29
 */

@Component
public class CommentL2Validator {

    private final PermissionService permissionService;

    private final int DEL_LEVEL = 3;

    public CommentL2Validator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void multiSetRights(@NotNull CommentL2 comment, @NotNull List<CommentL2> list, Operator operator) {
        boolean delete = checkDeletePermission(comment, operator);
        for (int i = 0; i < list.size(); ++i)
            list.get(i).setRights(new CommentRight().setDelete(delete));
    }

    public boolean checkCreatePermission(CommentL2 comment, @NotNull Operator operator) {
        if (operator.getId().equals(-1))
            return false;
        Date talkBanTo = operator.getUser().getTalkBanTo();
        if (talkBanTo != null && talkBanTo.before(new Date()))
            return false;
        return true;
    }

    public boolean checkDeletePermission(@NotNull CommentL2 comment, @NotNull Operator operator) {
        if (operator.getId().equals(-1))
            return false;
        if (operator.getUserId().equals(comment.getCreatorId()))
            return true;
        Integer categoryId = comment.getL1().getPost().getCategoryIdForRight();
        Integer permissionLevel = permissionService.readPermissionLevel(categoryId, operator).getObject();
        return permissionLevel != null && permissionLevel >= DEL_LEVEL;
    }

}
