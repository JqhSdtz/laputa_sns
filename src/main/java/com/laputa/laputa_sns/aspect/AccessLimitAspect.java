package com.laputa.laputa_sns.aspect;

import com.laputa.laputa_sns.annotation.AccessLimit;
import com.laputa.laputa_sns.annotation.AccessLimitTarget;
import com.laputa.laputa_sns.annotation.AccessLimits;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import com.laputa.laputa_sns.common.RedisPrefix;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.util.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * @author JQH
 * @since 下午 8:38 21/03/01
 */

@Slf4j
@Component
@Aspect
public class AccessLimitAspect implements ApplicationRunner {

    private static final DefaultRedisScript<List<List<Object>>> globalTargetRegisterScript =
            new DefaultRedisScript(ResourceUtil.getString("/lua/access_limit/globalTargetRegister.lua"), List.class);
    private String methodRegisterKey = "METHOD";
    private Map<String, Long> methodRegisterMap = new HashMap<>();

    private final StringRedisTemplate redisTemplate;

    public AccessLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    /**向redis中注册需要限制访问的方法*/
    public void run(ApplicationArguments args) {
        Reflections reflections = new Reflections(new MethodAnnotationsScanner());
        Set<Method> methodSet = reflections.getMethodsAnnotatedWith(AccessLimits.class);
        List<String> methodNameList = new ArrayList<>();
        for (Method method : methodSet) {
            String methodName = method.getClass().getName() + ":" + method.getName();
            methodNameList.add(methodName);
        }
        List<List<Object>> resList = redisTemplate.execute(globalTargetRegisterScript, Arrays.asList(methodRegisterKey), methodNameList.toArray());
        int successCnt = 0;
        for (List<Object> res : resList) {
            if (res.size() == 2) {
                ++successCnt;
                methodRegisterMap.put((String) res.get(0), (Long) res.get(1));
            }
        }
        log.info("注册访问限制方法" + successCnt + "个");
    }

    @Pointcut("@annotation(com.laputa.laputa_sns.annotation.AccessLimits)")
    public void point() {
    }

    @Pointcut("@annotation(com.laputa.laputa_sns.annotation.NeedLogin)")
    public void needLoginPoint() {
    }

    @Around("needLoginPoint()")
    public Result testIfLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        boolean hasLogin = false;
        for (int i = 0; i < args.length; ++i) {
            if (args[i] instanceof Operator) {
                Operator operator = (Operator) args[i];
                hasLogin = operator.getUser() != null && !operator.getUserId().equals(-1);
                break;
            }
        }
        if (hasLogin) {
            return (Result) joinPoint.proceed();
        } else {
            return new Result(Result.FAIL).setErrorCode(1010220202).setMessage("请求失败，该请求需要登录");
        }
    }

    @Around("point()")
    public Result parseAccessLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sign = (MethodSignature) joinPoint.getSignature();
        Method method = sign.getMethod();
        Parameter[] parameters = method.getParameters();
        Operator operator = null;
        AbstractBaseEntity target = null;
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; ++i) {
            Parameter parameter = parameters[i];
            Object argValue = args[i];
            if (argValue instanceof Operator) {
                operator = (Operator) argValue;
            } else if (parameter.isAnnotationPresent(AccessLimitTarget.class)) {
                AccessLimitTarget limitTarget = parameter.getAnnotation(AccessLimitTarget.class);
                if (argValue instanceof AbstractBaseEntity) {
                    target = (AbstractBaseEntity) argValue;
                    if (!"".equals(limitTarget.byMethod())) {
                        String methodName = limitTarget.byMethod();
                        target = (AbstractBaseEntity) target.getClass().getDeclaredMethod(methodName).invoke(target);
                    }
                }
            }
        }
        if (operator == null) {
            throw new Exception("operator must be defined for access limit");
        }
        if ((operator.getUserId() != null && operator.getUserId() < 0) || operator.isSuperAdmin()) {
            // 未登录或超级管理员不受任何限制
            return (Result) joinPoint.proceed();
        }
        AccessLimits limits = method.getAnnotation(AccessLimits.class);
        Arrays.sort(limits.value(), Comparator.comparing(AccessLimit::per).reversed());
        boolean result = doAccessLimit(method, target, operator, limits);
        if (result) {
            return (Result) joinPoint.proceed();
        } else {
            return new Result(Result.FAIL).setErrorCode(1010220201).setMessage("请求过于频繁，请稍后再试").setOperator(operator);
        }
    }

    /**
     * 执行访问限制，返回true表示允许访问，反之禁止访问
     *
     * @param target
     * @param operator
     * @param limits
     * @return
     */
    private boolean doAccessLimit(Method method, AbstractBaseEntity target, Operator operator, AccessLimits limits) throws Exception {
        String methodName = method.getClass().getName() + ":" + method.getName();
        Long methodCode = methodRegisterMap.get(methodName);
        String targetId;
        if (target != null) {
            targetId = methodCode + "!" + target.getId();
        } else {
            targetId = methodCode.toString();
        }
        // 从redis中取出该用户对于该目标的访问记录，结果为字符串，格式为
        // 时间单位1@已访问次数1@计数开始时间戳1#时间单位2@已访问次数2@计数开始时间戳2
        // 时间单位为s/m/h，表示秒、分钟、小时，已访问次数为该时间单位内已访问的次数
        // 计数开始时间戳为已访问次数开始计数时的时间戳
        String lastAccessStr = (String) redisTemplate.opsForHash().get(RedisPrefix.OPERATOR_ACCESS_MAP + operator.getId(), targetId);
        Map<String, Long[]> accessMap = new HashMap<>();
        boolean isFirstAccess = false;
        if (lastAccessStr == null) {
            isFirstAccess = true;
        } else {
            // 解析访问记录字符串
            // 获取访问记录的多个时间单位部分
            String[] accessStrParts = lastAccessStr.split("#");
            for (String str : accessStrParts) {
                // 对于每个时间单位，获取时间单位标志、已访问次数和计数开始时间戳
                String[] parts = str.split("@");
                if (parts.length < 3)
                    continue;
                // 将对于该时间单位的已访问次数和计数开始时间戳放到map中
                accessMap.put(parts[0], new Long[]{Long.valueOf(parts[1]), Long.valueOf(parts[2])});
            }
        }
        long now = new Date().getTime();
        boolean result = true;
        for (AccessLimit limit : limits.value()) {
            String limitTimeUnit = limit.per().getValue();
            if (isFirstAccess) {
                accessMap.put(limitTimeUnit, new Long[]{now, 0L});
                continue;
            }
            Long lastAccessTime = accessMap.get(limitTimeUnit)[0];
            Long hasAccessCount = accessMap.get(limitTimeUnit)[1];
            if (lastAccessTime == null) {
                accessMap.put(limitTimeUnit, new Long[]{now, 0L});
                continue;
            }
            int timeDiff = limit.per().getTimeDiff();
            if (now - lastAccessTime > timeDiff) {
                // 已经超过访问限制间隔时间，允许访问
                accessMap.put(limitTimeUnit, new Long[]{now, 0L});
                continue;
            }
            int limitValue = limit.value();
            if (hasAccessCount <= limitValue) {
                // 没有超过允许访问次数
                accessMap.put(limitTimeUnit, new Long[]{lastAccessTime, hasAccessCount + 1});
                continue;
            }
            // 否则，没有超过限制访问间隔时间，且达到允许访问次数，则禁止访问
            result = false;
        }
        String accessStr = "";
        boolean isFirst = true;
        for (Map.Entry<String, Long[]> entry : accessMap.entrySet()) {
            String timeUnit = entry.getKey();
            Long[] accessValue = entry.getValue();
            Long accessCount = accessValue[0];
            Long accessTime = accessValue[1];
            String str = timeUnit + "@" + accessCount + "@" + accessTime;
            accessStr += (isFirst ? "" : "#") + str;
            isFirst = false;
        }
        redisTemplate.opsForHash().put(RedisPrefix.OPERATOR_ACCESS_MAP + operator.getId(), targetId, accessStr);
        return result;
    }

    /**
     * 每天清空访问记录
     */
    @Scheduled(cron = "0 10 4 * * ?")
    public void dailyFlushRedisAccess() {
        Set<String> keys = RedisUtil.scanAllKeys(redisTemplate, RedisPrefix.OPERATOR_ACCESS_MAP);
        redisTemplate.delete(keys);
    }
}