package com.laputa.laputa_sns.model.param.wx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author JQH
 * @since 下午 8:41 20/06/11
 */

@Getter
@Setter
public class CodeAndUserInfo {
    private String code;
    @JsonProperty("user_info")
    private UserInfoRes userInfo;
}
