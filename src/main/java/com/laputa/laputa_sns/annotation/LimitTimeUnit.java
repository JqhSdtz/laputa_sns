package com.laputa.laputa_sns.annotation;

/**
 * @author JQH
 * @since 下午 11:55 21/03/02
 */
public enum LimitTimeUnit {

    // 这里的顺序一定是由小到大，因为要先验证间隔大的时间，再验证间隔小的时间
    // 添加的注解的时候不需要注意顺序
    SECOND("s", 1000), MINUTE("m", 60000), HALF_HOUR("hh", 1800000),  HOUR("h", 3600000);

    /**
     *  时间单位描述，为了减少Redis存储占用，要尽可能短
     */
    private String value;

    /**
     * 时间间隔，单位毫秒
     */
    private int timeDiff;

    LimitTimeUnit(String value, int timeDiff) {
        this.value = value;
        this.timeDiff = timeDiff;
    }

    public String getValue() {
        return value;
    }

    public int getTimeDiff() {
        return timeDiff;
    }
}
