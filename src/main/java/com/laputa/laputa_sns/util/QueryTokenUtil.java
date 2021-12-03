package com.laputa.laputa_sns.util;

import com.laputa.laputa_sns.common.AbstractBaseEntity;
import com.laputa.laputa_sns.common.QueryParam;
import com.laputa.laputa_sns.common.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

/**
 * 查询Token工具类，用于在需要验证的操作中验证和设置token
 * 主要是分页查询场景，用于避免系统资源被恶意占用
 * @author JQH
 * @since 下午 4:04 20/03/26
 */
public class QueryTokenUtil {

    @NotNull
    /**newStartId为空则自动从resList中获取*/
    public static <T extends AbstractBaseEntity> String generateQueryToken(@NotNull T param, @NotNull List<T> resList, @NotNull QueryParam queryParam, int indexType, String hmacKey) {
        boolean endOfQuery = queryParam == null ? false : queryParam.isEndOfQuery();
        boolean abandon = endOfQuery || resList.size() == 0;//刷到底了，该token弃用
        Integer newStartId = queryParam == null ? null : queryParam.getStartId();
        if (newStartId == null)
            newStartId = (abandon || resList.get(resList.size() - 1) == null) ? null : resList.get(resList.size() - 1).getId();
        Integer newFrom = queryParam == null ? null : queryParam.getFrom();
        if (newFrom == null)
            newFrom = abandon ? null : (param.getQueryParam().getFrom() + resList.size());
        String newStartValue = queryParam == null ? null : queryParam.getStartValue();
        String newAddition = queryParam == null ? null : queryParam.getAddition();
        String ori = (abandon ? '1' : '0') + "#" + new Date().getTime() + "#" + param.getEntityType() + "#" + indexType
                + "#" + param.getOfId() + "#" + (newStartId == null ? 0 : newStartId) + "#" + newStartValue + "#" + (newFrom == null ? 0 : newFrom) + "#" + newAddition;
        String sign = CryptUtil.hmac(hmacKey, ori);
        return ori + "@" + sign;
    }

    public static Result<Object> validateTokenAndSetQueryParam(@NotNull AbstractBaseEntity param, int indexType, String hmacKey) {
        if (param.getQueryParam() == null)
            param.setQueryParam(new QueryParam());
        String token = param.getQueryParam().getQueryToken();
        if (token != null) success:{
            String[] splitStr = token.split("@");
            if (splitStr.length != 2)
                return new Result<Object>(Result.FAIL).setErrorCode(1010140201).setMessage("Token格式不正确");//格式不正确，直接返回错误
            if (!splitStr[1].equals(CryptUtil.hmac(hmacKey, splitStr[0])))
                return new Result<Object>(Result.FAIL).setErrorCode(1010140202).setMessage("Token校验失败");//HMAC签名验证失败，直接返回错误
            String[] splitParam = splitStr[0].split("#");
            if (splitParam.length != 9)
                break success;//参数格式不正确，可能是后端的问题，设为默认值
            if ("1".equals(splitParam[0]))//产生token是设置的abandon位，说明刷到底了
                return new Result<Object>(Result.FAIL).setErrorCode(1010140203).setMessage("没有更多内容");
            if (new Date().getTime() - Long.valueOf(splitParam[1]) < 500)
                return new Result<Object>(Result.FAIL).setErrorCode(1010140204).setMessage("刷新过于频繁");;//两次更新时间小于500毫秒，直接返回错误
            if (!param.getEntityType().equals(splitParam[2]))
                return new Result<Object>(Result.FAIL).setErrorCode(1010140205).setMessage("queryToken类型不符");
            if (!String.valueOf(indexType).equals(splitParam[3]) || !String.valueOf(param.getOfId()).equals(splitParam[4]))
                break success;//indexType和categoryId和参数不相等，可能是前端传了其他次请求的token，当前参数设为默认
            Integer startId = notNull(splitParam[5]) ? Integer.valueOf(splitParam[5]) : null;
            String startValue = notNull(splitParam[6]) ? splitParam[6] : null;
            Integer from = notNull(splitParam[7]) ? Integer.valueOf(splitParam[7]) : null;
            String addition = notNull(splitParam[8]) ? splitParam[8] : null;
            param.getQueryParam().setStartId(startId).setStartValue(startValue).setFrom(from).setAddition(addition);
            return new Result<Object>(Result.SUCCESS);
        }
        param.getQueryParam().setStartId(0).setFrom(0).setStartValue(null).setAddition(null);
        return new Result<Object>(Result.SUCCESS);
    }

    private static boolean notNull(@NotNull String v) {
        return v != null && v.length() != 0 && !"null".equals(v);
    }

}
