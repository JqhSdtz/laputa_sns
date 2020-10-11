package com.laputa.laputa_sns.model.param.wx;

/**
 * @author JQH
 * @since 下午 8:43 20/06/11
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Code2SessionResult {
    private String openid;
    @JsonProperty("session_key")
    private String sessionKey;
    private String unionid;
    @JsonProperty("errorcode")
    private String errorCode;
    @JsonProperty("errmsg")
    private String errMsg;
}
