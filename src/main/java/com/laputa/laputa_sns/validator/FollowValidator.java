package com.laputa.laputa_sns.validator;

import com.laputa.laputa_sns.model.entity.Follow;
import com.laputa.laputa_sns.model.entity.Operator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * @author JQH
 * @since 下午 9:35 20/02/20
 */

@Component
public class FollowValidator {

    public boolean checkCreatePermission(@NotNull Follow follow, Operator operator) {
        if(operator .getId().equals(-1)) {
            return false;
        }
        return true;
    }

    public boolean checkUpdatePermission(@NotNull Follow follow, Operator operator) {
        if(operator .getId().equals(-1)) {
            return false;
        }
        return true;
    }

    public boolean checkDeletePermission(@NotNull Follow follow, Operator operator) {
        if(operator .getId().equals(-1)) {
            return false;
        }
        return true;
    }
}
