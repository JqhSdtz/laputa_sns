package com.laputa.laputa_sns.model.param.wx;

import lombok.Getter;
import lombok.Setter;

/**
 * @author JQH
 * @since 下午 8:41 20/06/11
 */

@Getter
@Setter
public class UserInfo {
    private String nickName;
    private String avatarUrl;
    private Integer gender;
    private String country;
    private String province;
    private String city;
    private String language;
}
