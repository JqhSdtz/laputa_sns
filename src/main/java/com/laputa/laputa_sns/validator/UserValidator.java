package com.laputa.laputa_sns.validator;

import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.User;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * @author JQH
 * @since 下午 4:24 20/02/15
 */

@Component
public class UserValidator {

    public boolean checkUpdatePermission(@NotNull User user, @NotNull Operator operator) {
        return user.getId().equals(operator.getUserId());
    }

    public boolean checkSetTalkBanToPermission(@NotNull Operator operator) {
        if (!operator.isAdmin()) {
            return false;
        }
        Integer permLevel = operator.getPermissionMap().get(CategoryService.GROUND_ID);
        return permLevel != null && permLevel >= AdminLevel.TALK_BAN;
    }

    public boolean checkRedefineUserStatePermission(Operator operator) {
        return ProgOperatorManager.isProgOperator(operator);
    }

}
