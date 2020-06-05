package com.laputa.laputa_sns.util;

import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.model.Operator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author JQH
 * @since 下午 12:01 20/02/15
 */
public class ProgOperatorManager {
    private static Map<Integer, Class> progOperatorMap = new HashMap<>();
    private static int progId = -1;

    public static Operator unLoggedOperator = new Operator(-1).setUserName("unLoggedOperator");

    @NotNull
    public static Operator register(@NotNull Class progClass) {
        Operator progOperator = new Operator(--progId).setUserName(progClass.getName());
        progOperatorMap.put(progOperator.getUserId(), progClass);
        return progOperator;
    }

    public static boolean isProgOperatorOfClass(@NotNull Operator progOperator, Class progClass) {
        if (!progOperatorMap.containsKey(progOperator.getUserId()))
            return false;
        return progOperatorMap.get(progOperator.getUserId()).equals(progClass);
    }

    public static boolean isProgOperator(Operator operator) {
        return progOperatorMap.containsKey(operator.getUserId());
    }
}
