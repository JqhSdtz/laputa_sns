package com.laputa.laputa_sns.helper;

import com.laputa.laputa_sns.common.Result;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author JQH
 * @since 上午 10:52 20/04/25
 */

public class SimpleQueryHelper {

    public interface SelectOneCallBack {
        String selectOne(Integer id);
    }

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final int timeOut;

    public SelectOneCallBack selectOneCallBack;

    private final Random random = new Random();
    private final int refreshProbability = 100;//1%的概率刷新key

    public SimpleQueryHelper(StringRedisTemplate redisTemplate, String keyPrefix, int timeOut) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.timeOut = timeOut;
    }

    private String getKey(Integer id) {
        return keyPrefix + ":" + id;
    }

    public Result<String> readValue(Integer id) {
        String key = getKey(id);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            value = selectOneCallBack.selectOne(id);
            if (value == null)
                return new Result(Result.FAIL).setErrorCode(1010180101).setMessage("数据库操作失败");
            redisTemplate.opsForValue().set(key, value, timeOut, TimeUnit.MINUTES);
        } else if (random.nextInt() % refreshProbability == 0)
            redisTemplate.expire(key, timeOut, TimeUnit.MINUTES);
        return new Result(Result.SUCCESS).setObject(value);
    }

    public void setRedisValue(Integer id, String value) {
        redisTemplate.opsForValue().set(getKey(id), value, timeOut, TimeUnit.MINUTES);
    }

}
