package com.laputa.laputa_sns.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author JQH
 * @since 下午 12:50 20/02/05
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class QueryParam {
    public final static int EXIST = 0;
    public final static int BASIC = 1;
    public final static int FULL = 2;
    public final static int SPEC1 = 3;

    @JsonIgnore
    private Integer queryType;

    @JsonIgnore
    private Character queryNotIndexed;

    @JsonIgnore
    private int isPaged;

    @JsonIgnore
    private int useStartIdAtSql;

    @JsonIgnore
    private String orderBy;

    @JsonIgnore
    private String orderDir;

    @JsonProperty("create_time")
    private Boolean createTime;

    @JsonProperty("query_token")
    private String queryToken;

    @JsonProperty("query_num")
    private Integer queryNum;

    @JsonProperty("start_value")
    private String startValue;

    @JsonProperty("start_id")
    private Integer startId;

    private Integer from;
    private Integer to;

    private String addition;
}
