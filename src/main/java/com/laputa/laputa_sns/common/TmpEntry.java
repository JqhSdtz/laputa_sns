package com.laputa.laputa_sns.common;

import lombok.Getter;

/**
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

    public int hashCode() {
        return id;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof TmpEntry))
            return false;
        return this.id.equals(((TmpEntry) obj).id);
    }
}
