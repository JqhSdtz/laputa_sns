package com.laputa.laputa_sns.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.User;
import com.laputa.laputa_sns.service.OSSService;
import com.laputa.laputa_sns.service.OperatorService;
import com.laputa.laputa_sns.service.UserService;
import com.laputa.laputa_sns.util.RestUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Map;

/**
 * @author JQH
 * @since 上午 10:08 21/03/13
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/qq")
public class QQController {

    private final String authUrl = "https://graph.qq.com/oauth2.0/token";
    private final String getOpenIdUrl = "https://graph.qq.com/oauth2.0/me";
    private final String getUserInfoUrl = "https://graph.qq.com/user/get_user_info";

    @Value("${qq-client-id}")
    private String clientId;

    @Value("${qq-secret-key}")
    private String secretKey;

    @Value("${qq-redirect-url}")
    private String redirectUrl;

    private ObjectMapper resultMapper = new ObjectMapper();

    private final UserService userService;
    private final OSSService ossService;
    private final OperatorService operatorService;

    public QQController(UserService userService, OSSService ossService, OperatorService operatorService) {
        this.userService = userService;
        this.ossService = ossService;
        this.operatorService = operatorService;
    }

    @SneakyThrows
    @RequestMapping(value = "/login/{code}", method = RequestMethod.POST)
    @SuppressWarnings("unchecked")
    public Result<Operator> login(@PathVariable String code, HttpServletRequest request, HttpServletResponse response, @RequestAttribute Operator operator) {
        MultiValueMap<String, Object> getTokenParam = new LinkedMultiValueMap<>();
        getTokenParam.add("grant_type", "authorization_code");
        getTokenParam.add("client_id", clientId);
        getTokenParam.add("client_secret", secretKey);
        getTokenParam.add("code", code);
        getTokenParam.add("redirect_uri", redirectUrl);
        getTokenParam.add("fmt", "json");
        ResponseEntity<String> tokenEntity = RestUtil.get(authUrl, getTokenParam);
        String tokenResult = tokenEntity.getBody();
        Map<String, Object> tokenJsonResult = resultMapper.readValue(tokenResult, Map.class);
        Integer error = (Integer) tokenJsonResult.get("error");
        if (error != null) {
            log.error("QQ登录失败！错误信息:" + tokenResult);
            return new Result<Operator>(Result.FAIL).setErrorCode(1010230101).setMessage(tokenResult);
        }
        String accessToken = tokenJsonResult.get("access_token").toString();
        MultiValueMap<String, Object> getOpenIdParam = new LinkedMultiValueMap<>();
        getOpenIdParam.add("access_token", accessToken);
        getOpenIdParam.add("fmt", "json");
        ResponseEntity<String> openIdEntity = RestUtil.get(getOpenIdUrl, getOpenIdParam);
        String openIdResult = openIdEntity.getBody();
        Map<String, Object> openIdJsonResult = resultMapper.readValue(openIdResult, Map.class);
        error = (Integer) openIdJsonResult.get("error");
        if (error != null) {
            log.error("QQ登录获取OpenID失败！错误信息:" + openIdResult);
            return new Result<Operator>(Result.FAIL).setErrorCode(1010230102).setMessage(openIdResult);
        }
        String openId = openIdJsonResult.get("openid").toString();
        Result<User> userResult = userService.readUserWithQqOpenId(openId);
        User user = userResult.getObject();
        if (user == null) {
            MultiValueMap<String, Object> getUserInfoParam = new LinkedMultiValueMap<>();
            getUserInfoParam.add("access_token", accessToken);
            getUserInfoParam.add("oauth_consumer_key", clientId);
            getUserInfoParam.add("openid", openId);
            ResponseEntity<String> userInfoEntity = RestUtil.get(getUserInfoUrl, getUserInfoParam);
            String userInfoResult = userInfoEntity.getBody();
            Map<String, Object> userInfoJsonResult = resultMapper.readValue(userInfoResult, Map.class);
            if (((Integer) userInfoJsonResult.get("ret")) != 0) {
                log.error("QQ登录获取用户信息失败！错误信息:" + userInfoResult);
                return new Result<Operator>(Result.FAIL).setErrorCode(1010230103).setMessage(userInfoResult);
            }
            int randomBase = 100;
            String userName;
            do {
                if (randomBase == 0) {
                    log.error("尝试创建用户名失败！极小概率事件，请检查是否有异常。");
                    return new Result<Operator>(Result.FAIL).setErrorCode(1010230104).setMessage("尝试创建用户名失败！");
                }
                userName = userInfoJsonResult.get("nickname") + String.valueOf((int) (Math.random() * randomBase));
                randomBase *= 10;
            } while (userService.checkNickName(userName).getState() != Result.SUCCESS);
            user = new User(-1).setQqOpenId(openId).setNickName(userName);
            operator.setUser(user);
            String avatarUrl = userInfoJsonResult.get("figureurl_qq_1").toString();
            BufferedImage image = ImageIO.read(new URL(avatarUrl));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", output);
            byte[] imageFile = output.toByteArray();
            Result<String> uploadResult = ossService.uploadImgSync(imageFile, OSSService.AVATAR, operator);
            if (uploadResult.getState() == Result.SUCCESS) {
                user.setRawAvatar(uploadResult.getObject());
            }
            Result<Operator> registerResult = operatorService.register(user, request, response);
            if (registerResult.getState() == Result.SUCCESS) {
                registerResult.getObject().setFromRegister(true);
            }
            return registerResult.setOperator(registerResult.getObject());
        } else {
            Result<Operator> loginResult = operatorService.login(user, request, response);
            if (loginResult.getState() == Result.SUCCESS) {
                loginResult.getObject().setFromLogin(true);
            }
            return loginResult.setOperator(loginResult.getObject());
        }
    }
}
