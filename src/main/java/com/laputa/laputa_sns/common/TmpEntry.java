package com.laputa.laputa_sns.common;

import lombok.Getter;

/**
 * id和value的一个键值对
 * @author JQH
 * @since 上午 11:59 20/03/16
 */

@Getter
public class TmpEntry {
    Integer id;
    Object value;

    public TmpEntry(Integer id, Object value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TmpEntry)) {
            return false;
        }
        return this.id.equals(((TmpEntry) obj).id);
    }
}
