package com.laputa.laputa_sns.interceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laputa.laputa_sns.common.Globals;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.OperatorService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author JQH
 * @since 下午 12:25 20/02/14
 */

@Slf4j
@Component
public class ValidateInterceptor implements HandlerInterceptor {

    private final OperatorService operatorService;

    public ValidateInterceptor(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @Value("${pass-token-through-header}")
    private boolean passTokenThroughHeader;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!Globals.ready()) {
            writeResponse(response, "维护中");
            return false;
        }
        String servletPath = request.getServletPath();
        if ("api/operator/login".equals(servletPath) || "api/operator/register".equals(servletPath)
                || "api/qq/login".equals(servletPath)) {
            //不拦截注册登录请求
            return true;
        }
        String cookieStr = getTokenFromCookie(request.getCookies());
        // 优先从cookie中获取token，没有的话从请求头里获取
        if (cookieStr == null && passTokenThroughHeader) {
            cookieStr = request.getHeader("X-LPT-USER-TOKEN");
        }
        String message = null;
        if (cookieStr != null && cookieStr != "") {
            loadOperator:{
                String[] str = cookieStr.split("@");
                if (str.length < 2) {
                    message = "Token格式错误";
                    break loadOperator;
                }
                Integer userId;
                try {
                    userId = Integer.valueOf(str[0]);
                } catch (NumberFormatException e) {
                    message = "用户ID格式错误";
                    break loadOperator;
                }
                Operator operator = operatorService.readOperatorWithToken(userId, response);
                if (operator == null) {
                    message = "登录已过期，请重新登陆";
                    break loadOperator;
                }
                if (!str[1].equals(operator.getToken())) {
                    message = "Token不正确";
                    break loadOperator;
                }
                Result<Operator> operatorResult = operatorService.loadOperator(userId, operator, request, response);
                if (operatorResult.getState() == Result.FAIL) {
                    message = operatorResult.getMessage();
                    break loadOperator;
                }
    //            if (operator.getLastAccessTime() != null && System.currentTimeMillis() - operator.getLastAccessTime() < 200) {
    //                message = "操作过于频繁";
    //                break loadOperator;
    //            }
                request.setAttribute("operator", operatorResult.getObject());
                return true;
            }
        }
        request.setAttribute("operator", new Operator(-1).setLogMessage(message));
        return true;
    }

    @Nullable
    private String getTokenFromCookie(@NotNull Cookie[] cookies) {
        // 注意，cookie中可能包含多个token，以最后一个为准
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        String token = null;
        for (int i = 0; i < cookies.length; ++i) {
            if ("token".equals(cookies[i].getName())) {
                token = cookies[i].getValue();
            }
        }
        return token;
    }

    private void writeResponse(@NotNull HttpServletResponse response, String message) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        Result<Object> result = new Result<Object>(Result.FAIL).setMessage(message);
        try {
            String str = new ObjectMapper().writeValueAsString(result);
            response.getWriter().write(str);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage());
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

}
