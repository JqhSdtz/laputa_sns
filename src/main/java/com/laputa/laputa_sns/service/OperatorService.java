package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.common.RedisPrefix;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.helper.RedisHelper;
import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.model.User;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Map;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
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
    private final StringRedisTemplate redisTemplate;
    private final RedisHelper<Operator> redisHelper;

    private final int operatorTimeOut;

    private final Operator progOperator = ProgOperatorManager.register(OperatorService.class);

    public OperatorService(@Lazy UserService userService, @Lazy PermissionService permissionService, PostNewsService postNewsService, NoticeService noticeService, StringRedisTemplate redisTemplate, @NotNull Environment environment) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.postNewsService = postNewsService;
        this.noticeService = noticeService;
        this.redisTemplate = redisTemplate;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setFilterProvider(new SimpleFilterProvider().addFilter("OperatorFilter", SimpleBeanPropertyFilter
                .filterOutAllExcept("user_id", "token", "permission_map", "unread_news_cnt", "unread_notice_cnt", "last_access_time")));
        operatorTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.operator"));
        this.redisHelper = new RedisHelper(RedisPrefix.ONLINE_OPERATOR, objectMapper, operatorTimeOut, null, null, null, null, Operator.class, redisTemplate);
    }

    public Operator getOnlineOperator(Integer userId) {
        return redisHelper.getEntity(userId, false, false);
    }

    public String getBasicKeyPrefix() {
        return redisHelper.getBasicKey("*");
    }

    public void updateOnlineOperator(Operator operator) {
        redisHelper.setEntity(operator, false);
    }

    private void setToken(@NotNull Operator operator, @NotNull HttpServletResponse response) {
        String token = CryptUtil.randUrlSafeStr(32, true);
        operator.setToken(token);
        if (operator.getUser() != null)
            operator.getUser().setToken(token);
        Cookie cookie = new Cookie("token", operator.getUserId() + "@" + token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        //cookie.setSecure(true);
        response.addCookie(cookie);
    }

    @Nullable
    public Operator readOperatorWithToken(Integer userId, HttpServletResponse response) {
        Operator operator = redisHelper.getEntity(userId, false, false);
        if (operator == null) {//缓存中没有该用户，从数据库中读取
            Result<User> userResult = userService.readUserWithToken(userId);
            if (userResult.getState() == FAIL)
                return null;
            Operator resOperator = new Operator().setUser(userResult.getObject()).setToken(userResult.getObject().getToken());
            return resOperator.setFromLogin(true);//返回的是包含旧的token的operator
        } else
            return operator;
    }

    public Result<Operator> login(@NotNull User paramUser, HttpServletResponse response) {
        Result<User> loginResult = userService.login(paramUser);
        if (loginResult.getState() == FAIL)
            return (Result<Operator>) (Result) loginResult;
        User resUser = loginResult.getObject();
        Operator operator = new Operator();
        operator.setUser(resUser).setFromLogin(true);
        afterLogin(operator, response, false, false);
        return new Result(SUCCESS).setObject(operator);
    }

    public Result<Operator> register(User paramUser, HttpServletResponse response) {
        Operator operator = new Operator();
        operator.setUser(paramUser);
        setToken(operator, response);
        Result<Integer> registerResult = userService.createUser(paramUser);
        if (registerResult.getState() == FAIL)
            return (Result<Operator>) (Result) registerResult;
        operator.setUserId(registerResult.getObject()).setFromLogin(true);
        afterLogin(operator, response, false, true);
        return new Result(SUCCESS).setObject(operator);
    }

    public Result logout(@NotNull Operator operator, @NotNull HttpServletResponse response) {
        redisHelper.removeEntity(operator.getUserId());
        Cookie delCookie = new Cookie("token", "");
        delCookie.setPath("/");
        delCookie.setHttpOnly(true);
        response.addCookie(delCookie);
        return Result.EMPTY_SUCCESS;
    }

    private Result afterLogin(@NotNull Operator operator, HttpServletResponse response, boolean fromLoad, boolean fromRegister) {
        User user = operator.getUser();
        if (user.getState() != null && user.getState() > 0) {//该用户涉及管理权限
            Result<Map<Integer, Integer>> permissionMapResult = permissionService.readPermissionMapOfUser(user.getId(), progOperator);
            if (permissionMapResult.getState() == FAIL)//获取权限映射表失败
                return permissionMapResult;
            operator.setPermissionMap(permissionMapResult.getObject());
        }
        setToken(operator, response);
        //operator.setLastAccessTime(new Date().getTime()).setFromLogin(null);
        Result afterLogin = userService.afterLogin(user, operator.getToken(), fromRegister);//更新数据库，增加登录次数
        if (afterLogin.getState() == FAIL)
            return afterLogin;
        if (!fromLoad)
            redisHelper.setEntity(operator, false);
        return Result.EMPTY_SUCCESS;
    }

    public Result<Operator> loadOperator(Integer userId, Operator operator, HttpServletResponse response) {
        if (operator == null)
            operator = getOnlineOperator(userId);
        if (operator == null)
            return new Result(FAIL).setErrorCode(1010130201).setMessage("用户离线");
        boolean fromLogin = operator.getFromLogin() != null && operator.getFromLogin();
        if (fromLogin) {
            afterLogin(operator, response, true, false);
        } else {
            Result<User> userResult = userService.readUserWithCounter(userId, operator);//获取用户基本信息
            if (userResult.getState() == FAIL)
                return (Result) userResult.setMessage("ID错误");
            operator.setUser(userResult.getObject());
        }
        long curTime = new Date().getTime();
        if (operator.getLastAccessTime() == null || curTime - operator.getLastAccessTime() > 1000) {//距上一次访问大于5秒，重新读取未读动态数
            Result<Integer> cntResult = postNewsService.readNewsCount(operator);
            if (cntResult.getState() == SUCCESS)
                operator.setUnreadNewsCnt(cntResult.getObject());
            operator.setUnreadNoticeCnt(noticeService.pullNoticeCnt(operator.getUserId()));
        }
        operator.setLastAccessTime(curTime);
        redisHelper.setEntity(operator, false);//加载成功，刷新Redis中的Operator
        return new Result(SUCCESS).setObject(operator);
    }

}
