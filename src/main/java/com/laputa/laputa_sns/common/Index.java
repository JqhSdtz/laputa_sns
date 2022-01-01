package com.laputa.laputa_sns.common;

import org.jetbrains.annotations.NotNull;

/**
 * 索引对象
 * @author JQH
 * @since 下午 5:43 20/02/22
 */

public class Index implements Comparable<Index> {
    private int id;
    private long value;

    public Index(int id, long value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return this.id;
    }

    public long getValue() {
        return this.value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    /**
     * this.value < o.value 或 this.id < o.id返回正数，在一些容器中即从大到小排序
     */
    @Override
    public int compareTo(@NotNull Index o) {
        //不管是按时间排序还是热度排序，都是值大的排前面
        if (this.value == o.value) {
            return o.id - this.id;
        }
        return o.value > this.value ? 1 : -1;
    }
}
