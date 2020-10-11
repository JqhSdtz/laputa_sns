package com.laputa.laputa_sns.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.model.entity.Operator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author JQH
 * @since 下午 1:04 20/02/08
 */

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
public class Result<ResultType> implements Serializable {
    public static final int SUCCESS = 1;
    public static final int FAIL = 0;

    public static final Result EMPTY_SUCCESS = new Result(SUCCESS);
    public static final Result EMPTY_FAIL = new Result(FAIL);

    private int state;

    @JsonProperty("error_code")
    private Integer errorCode;

    private String message;

    private ResultType object;

    private Operator operator;

    @JsonProperty("attached_token")
    private String attachedToken;

    public Result(int state) {
        this.state = state;
    }

    public String toString() {
        return " 错误码:" + errorCode + " 错误信息:" + message;
    }

}
