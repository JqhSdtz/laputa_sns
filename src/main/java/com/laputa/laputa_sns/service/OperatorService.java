package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.common.RedisPrefix;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.helper.RedisHelper;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.User;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * 操作者相关的服务
 *
 * @author JQH
 * @since 上午 9:50 20/02/19
 */

@Slf4j
@Service
public class OperatorService {

    private final UserService userService;
    private final PermissionService permissionService;
    private final PostNewsService postNewsService;
    private final NoticeService noticeService;
    private final RedisHelper<Operator> redisHelper;

    private final Operator progOperator = ProgOperatorManager.register(OperatorService.class);

    public OperatorService(@Lazy UserService userService, @Lazy PermissionService permissionService, PostNewsService postNewsService, NoticeService noticeService, StringRedisTemplate redisTemplate, @NotNull Environment environment) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.postNewsService = postNewsService;
        this.noticeService = noticeService;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setFilterProvider(new SimpleFilterProvider().addFilter("OperatorFilter", SimpleBeanPropertyFilter
                .filterOutAllExcept("user_id", "token", "permission_map", "unread_news_cnt", "unread_notice_cnt", "last_access_time")));
        int operatorTimeOut = Integer.parseInt(Objects.requireNonNull(environment.getProperty("timeout.redis.operator")));
        this.redisHelper = new RedisHelper(RedisPrefix.OPERATOR_ONLINE, objectMapper, operatorTimeOut, null, null, null, null, Operator.class, redisTemplate);
    }

    @Value("${pass-token-through-header}")
    private boolean passTokenThroughHeader;

    @Value("${pow-zero-length}")
    private int powZeroLength;

    public Operator getOnlineOperator(Integer userId) {
        return redisHelper.getEntity(userId, false, false);
    }

    public String getBasicKeyPrefix() {
        return redisHelper.getBasicKey("*");
    }

    public void updateOnlineOperator(Operator operator) {
        redisHelper.setEntity(operator, false);
    }

    private void setCookie(HttpServletRequest request, HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        // cookie.setHttpOnly(true);
        // 设置子域名共享cookie
        String[] domainParts = request.getServerName().split("\\.");
        if (domainParts.length >= 2) {
            int len = domainParts.length;
            String domainNamePrefix = domainParts[len - 2] + '.' + domainParts[len - 1];
            cookie.setDomain(domainNamePrefix);
        }
        // 一个月
        cookie.setMaxAge(2592000);
        //cookie.setSecure(true);
        response.addCookie(cookie);
    }

    private void setToken(@NotNull Operator operator, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        String token = CryptUtil.randUrlSafeStr(32, true);
        operator.setToken(token);
        if (operator.getUser() != null)
            operator.getUser().setToken(token);
        String cookieToken = operator.getUserId() + "@" + token;
        setCookie(request, response, "token", cookieToken);
        if (passTokenThroughHeader) {
            response.addHeader("X-LPT-USER-TOKEN", cookieToken);
        }
    }

    @Nullable
    public Operator readOperatorWithToken(Integer userId, HttpServletResponse response) {
        Operator operator = redisHelper.getEntity(userId, false, false);
        if (operator == null) {
            //缓存中没有该用户，从数据库中读取
            Result<User> userResult = userService.readUserWithToken(userId);
            if (userResult.getState() == FAIL)
                return null;
            Operator resOperator = new Operator().setUser(userResult.getObject()).setToken(userResult.getObject().getToken());
            //返回的是包含旧的token的operator
            return resOperator.setFromLogin(true);
        } else
            return operator;
    }

    public Result<Operator> login(@NotNull User paramUser, HttpServletRequest request, HttpServletResponse response) {
        User resUser;
        if (paramUser.getQqOpenId() != null && paramUser.getId() != null) {
            // 通过QQ登录，无需校验登录名和密码
            resUser = paramUser;
        } else {
            Result<User> loginResult = userService.login(paramUser);
            if (loginResult.getState() == FAIL)
                return (Result<Operator>) (Result) loginResult;
            resUser = loginResult.getObject();
        }
        Operator operator = new Operator();
        operator.setUser(resUser).setFromLogin(true);
        afterLogin(operator, request, response, false, false);
        return new Result(SUCCESS).setObject(operator);
    }

    public Result<Operator> registerWithPow(User paramUser, String extra64, String rand, String calRes,
                                            HttpServletRequest request, HttpServletResponse response) {
        String nickName = paramUser.getNickName();
        if (nickName == null) {
            return new Result(FAIL).setErrorCode(1010130202).setMessage("用户名不能为空");
        }
        if (calRes == null || calRes.length() < powZeroLength) {
            return new Result(FAIL).setErrorCode(1010130203).setMessage("工作量证明结果格式错误");
        }
        for (int i = 0; i < powZeroLength; ++i) {
            if (calRes.charAt(i) != '0') {
                return new Result(FAIL).setErrorCode(1010130204).setMessage("工作量证明结果验证失败");
            }
        }
        String name64 = new String(Base64.getEncoder().encode(paramUser.getNickName().getBytes(StandardCharsets.UTF_8)));
        String nameMd5 = CryptUtil.md5(name64);
        String testRes = CryptUtil.md5(nameMd5 + extra64 + rand);
        if (!testRes.equals(calRes)) {
            return new Result(FAIL).setErrorCode(1010130204).setMessage("工作量证明结果与参数不符");
        }
        return register(paramUser, request, response);
    }

    public Result<Operator> register(User paramUser, HttpServletRequest request, HttpServletResponse response) {
        Operator operator = new Operator();
        operator.setUser(paramUser);
        setToken(operator, request, response);
        Result<Integer> registerResult = userService.createUser(paramUser);
        if (registerResult.getState() == FAIL)
            return (Result<Operator>) (Result) registerResult;
        operator.setUserId(registerResult.getObject()).setFromLogin(true);
        afterLogin(operator, request, response, false, true);
        return new Result(SUCCESS).setObject(operator);
    }

    public Result logout(@NotNull Operator operator, HttpServletRequest request, HttpServletResponse response) {
        redisHelper.removeEntity(operator.getUserId());
        setCookie(request, response, "token", "");
        if (passTokenThroughHeader) {
            response.addHeader("X-LPT-USER-TOKEN", "");
        }
        return Result.EMPTY_SUCCESS;
    }

    private Result afterLogin(@NotNull Operator operator, HttpServletRequest request, HttpServletResponse response, boolean fromLoad, boolean fromRegister) {
        User user = operator.getUser();
        if (user.getState() != null && user.getState() > 0) {
            //该用户涉及管理权限
            Result<Map<Integer, Integer>> permissionMapResult = permissionService.readPermissionMapOfUser(user.getId(), progOperator);
            if (permissionMapResult.getState() == FAIL) {
                //获取权限映射表失败
                return permissionMapResult;
            }
            operator.setPermissionMap(permissionMapResult.getObject());
        }
        setToken(operator, request, response);
        //operator.setLastAccessTime(new Date().getTime()).setFromLogin(null);
        //更新数据库，增加登录次数
        Result afterLogin = userService.afterLogin(user, operator.getToken(), fromRegister);
        if (afterLogin.getState() == FAIL)
            return afterLogin;
        if (!fromLoad) {
            pullNewsAndNoticeCnt(operator);
            redisHelper.setEntity(operator, false);
        }
        return Result.EMPTY_SUCCESS;
    }

    public Result<Operator> loadOperator(Integer userId, Operator operator, HttpServletRequest request, HttpServletResponse response) {
        if (operator == null)
            operator = getOnlineOperator(userId);
        if (operator == null)
            return new Result(FAIL).setErrorCode(1010130201).setMessage("用户离线");
        boolean fromLogin = operator.getFromLogin() != null && operator.getFromLogin();
        if (fromLogin) {
            afterLogin(operator, request, response, true, false);
        } else {
            //获取用户基本信息
            Result<User> userResult = userService.readUserWithCounter(userId, operator);
            if (userResult.getState() == FAIL)
                return (Result) userResult.setMessage("ID错误");
            operator.setUser(userResult.getObject());
        }
        pullNewsAndNoticeCnt(operator);
        //加载成功，刷新Redis中的Operator
        redisHelper.setEntity(operator, false);
        return new Result(SUCCESS).setObject(operator);
    }

    /**
     * 获取操作者的动态和通知数量
     *
     */
    private void pullNewsAndNoticeCnt(Operator operator) {
        long curTime = new Date().getTime();
        if (operator.getLastAccessTime() == null || curTime - operator.getLastAccessTime() > 1000) {
            //距上一次访问大于1秒，重新读取未读动态数
            Result<Integer> cntResult = postNewsService.readNewsCount(operator);
            if (cntResult.getState() == SUCCESS)
                operator.setUnreadNewsCnt(cntResult.getObject());
            operator.setUnreadNoticeCnt(noticeService.pullNoticeCnt(operator.getUserId()));
        }
        operator.setLastAccessTime(curTime);
    }

}
