package com.laputa.laputa_sns.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

/**
 * @author JQH
 * @since 下午 2:46 20/02/05
 */

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
public abstract class AbstractBaseEntity implements Comparable<AbstractBaseEntity>, Serializable {

    @JsonProperty("entity_type")
    protected String entityType = "BASE";

    protected Integer id;

    protected Integer type;

    protected Integer state;

    /**
     * 从属于指定ID的对象
     */
    protected Integer ofId;

    protected Boolean fromDb;

    @JsonProperty(value = "op_comment", access = JsonProperty.Access.WRITE_ONLY)
    protected String opComment;

    protected Boolean deleted;

    @JsonProperty("create_time")
    protected Date createTime;

    @JsonProperty(value = "query_param", access = JsonProperty.Access.WRITE_ONLY)
    protected QueryParam queryParam;

    @Override
    public int compareTo(@NotNull AbstractBaseEntity o) {//TreeSet使用compareTo方法排序和比较相等
        return this.id - o.id;
    }

    @Override
    public boolean equals(@NotNull Object o) {
        if(!(o instanceof AbstractBaseEntity)) {
            // 判断是否是子类
            return false;
        }
        return this.id.equals(((AbstractBaseEntity)o).id);
    }

    @Override
    /**哈希码直接使用id，因为id不会重复，没有ID则返回随机值*/
    public int hashCode() {
        return this.id == null ? (int) Math.random() * 1000: this.id.intValue();
    }

    @JsonIgnore
    public boolean isValidOpComment() {
        return opComment != null && opComment.length() >= 5 && opComment.length() <= 256;
    }

}
