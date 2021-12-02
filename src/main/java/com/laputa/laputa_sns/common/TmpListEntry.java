package com.laputa.laputa_sns.common;

import java.util.List;

import lombok.Getter;

/**
 * id和value的一个键值对
 * @author JQH
 * @since 上午 11:59 20/03/16
 */

@Getter
public class TmpListEntry {
    Integer id;
    List<Object> values;

    public TmpListEntry(Integer id, List<Object> values) {
        this.id = id;
        this.values = values;
    }

    public int hashCode() {
        return id;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof TmpEntry))
            return false;
        return this.id.equals(((TmpEntry) obj).id);
    }
}
