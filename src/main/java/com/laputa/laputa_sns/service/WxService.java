package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.common.Secrets;
import com.laputa.laputa_sns.model.param.wx.Code2SessionResult;
import com.laputa.laputa_sns.model.param.wx.CodeAndUserInfo;
import com.laputa.laputa_sns.model.param.wx.UserInfoRes;
import com.laputa.laputa_sns.util.CryptUtil;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 微信相关的服务
 * @author JQH
 * @since 下午 7:37 20/06/11
 */

@Service
public class WxService {

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    private final String code2SessionUrl = "https://api.weixin.qq.com/sns/jscode2session" +
            "?appid=" + Secrets.WxAppId + "&secret=" + Secrets.WxAppSecret + "&grant_type=authorization_code&js_code=";

    public WxService() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
        restTemplate.getMessageConverters().add(converter);//设置支持Content-Type为text/plain的json内容
    }

    public Result login(CodeAndUserInfo codeAndUserInfo) {
        ResponseEntity<Code2SessionResult> responseEntity = restTemplate.getForEntity(code2SessionUrl + codeAndUserInfo.getCode(), Code2SessionResult.class);
        UserInfoRes userInfoRes = codeAndUserInfo.getUserInfo();
        String sessionKey = responseEntity.getBody().getSessionKey();
        String rawData = userInfoRes.getRawData();
        if (!userInfoRes.getSignature().equals(CryptUtil.sha1(rawData + sessionKey)))
            return new Result(Result.FAIL).setErrorCode(1010210201).setMessage("登录信息签名错误");

        return Result.EMPTY_SUCCESS;
    }
}
