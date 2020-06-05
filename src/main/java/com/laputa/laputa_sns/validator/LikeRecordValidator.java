package com.laputa.laputa_sns.validator;

import com.laputa.laputa_sns.model.LikeRecord;
import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * @author JQH
 * @since 下午 2:30 20/02/25
 */

@Component
public class LikeRecordValidator {

    public boolean checkCreatePermission(LikeRecord likeRecord, @NotNull Operator operator) {
        if(operator .getId().equals(-1))
            return false;
        return true;
    }

    public boolean checkDeletePermission(LikeRecord likeRecord, @NotNull Operator operator) {
        if(operator .getId().equals(-1))
            return false;
        return true;
    }

}
