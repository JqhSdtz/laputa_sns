package com.laputa.laputa_sns.model.param.wx;

import lombok.Getter;
import lombok.Setter;

/**
 * @author JQH
 * @since 下午 9:32 20/06/11
 */

@Getter
@Setter
public class UserInfoRes {
    private UserInfo userInfo;
    private String rawData;
    private String signature;
    private String encryptedData;
    private String iv;
    private String cloudId;
}
