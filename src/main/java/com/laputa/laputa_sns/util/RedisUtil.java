package com.laputa.laputa_sns.util;

import com.laputa.laputa_sns.common.QueryParam;
import com.laputa.laputa_sns.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.IOException;
import java.util.*;

/**
 * @author JQH
 * @since 下午 3:42 20/03/19
 */

@Slf4j
public class RedisUtil {
    private static final DefaultRedisScript<List<String>> zRevRangeUseStartIdFirstScript = new DefaultRedisScript(
            "if (redis.call('exists', KEYS[1]) == 0) then \n" + "\treturn nil end \n" + "local startId = tonumber(ARGV[1])\n" +
                    "local from = tonumber(ARGV[2])\n" + "if (startId ~= 0) then \n" + "\tlocal rank = redis.call('zrevrank', KEYS[1], ARGV[1])\n" +
                    "\tif (rank ~= nil and rank ~= false) then \n" + "\t\tfrom = rank + 1\n" + "\tend \n" + "end \n" + "if (ARGV[4] == '0') then\n" +
                    "\treturn redis.call('zrevrange', KEYS[1], from, from + tonumber(ARGV[3]) - 1) end\n" +
                    "return redis.call('zrevrange', KEYS[1], from, from + tonumber(ARGV[3]) - 1, 'WITHSCORES')", List.class);

    private static final DefaultRedisScript<List<String>> zRevRangeUseFromFirstScript = new DefaultRedisScript(
            "if (redis.call('exists', KEYS[1]) == 0) then \n" + "\treturn nil end \n" + "local from = tonumber(ARGV[1]) \n" +
                    "if (from > redis.call('zcard', KEYS[1])) then\n" + "\tlocal rank = redis.call('zrevrank', KEYS[1], ARGV[2]) \n" + "\tif (rank ~= nil and rank ~= false) then \n" +
                    "\t\tfrom = rank end\n" + "end \n" + "if (ARGV[4] == '0') then\n" + "\treturn redis.call('zrevrange', KEYS[1], from, from + tonumber(ARGV[3]) - 1) end\n" +
                    "return redis.call('zrevrange', KEYS[1], from, from + tonumber(ARGV[3]) - 1, 'WITHSCORES')", List.class);

    private static final DefaultRedisScript<List<Object>> zAddAndGetLastScript = new DefaultRedisScript(
            "local zCard = tonumber(redis.call('zcard', KEYS[1]))\n" + "local insertLen = #ARGV / 2\n" + "local limit = tonumber(KEYS[2])\n" +
                    "if (zCard + insertLen > limit) then\n" + "\tif (KEYS[3] == '0') then\n" + "\t\treturn 'f' end\n" + "\tlocal dim = zCard + insertLen - limit\n" +
                    "\tredis.call('zremrangebyrank', KEYS[1], 0, dim - 1)\n" + "\tif (insertLen > limit) then\n" + "\t\tlocal tmpTable = {}\n" +
                    "\t\tfor i = 1, limit do table.insert(tmpTable, tonumber(ARGV[i * 2 - 1])) table.insert(tmpTable, ARGV[i * 2]) end\n" +
                    "\t\tARGV = tmpTable\n" + "\tend\n" + "end\n" + "redis.call('zadd', KEYS[1], unpack(ARGV))\n" +
                    "return {redis.call('zrange', KEYS[1], 0, 0), redis.call('zcard', KEYS[1])}", List.class);

    private static final DefaultRedisScript<Long> zRemRangeByLex = new DefaultRedisScript("return redis.call('zremrangebylex', KEYS[1], ARGV[1], ARGV[2])", Long.class);

    @NotNull
    /**返回object数组result[]，长度为2，result[0]为zSet最后一个值，result[1]为zSet长度*/
    public static Object[] addToZSetAndGetLastAndLength(@NotNull StringRedisTemplate redisTemplate, String key, @NotNull Set<Tuple> valueSet, int limit, boolean replaceWhenFull) {
        if (valueSet == null || valueSet.size() == 0)
            return null;
        List<String> argv = new ArrayList(valueSet.size() * 2);
        for (Tuple tuple : valueSet) {
            argv.add(String.valueOf(tuple.getScore()));
            argv.add(new String(tuple.getValue()));
        }
        List<Object> resList = redisTemplate.execute(zAddAndGetLastScript, Arrays.asList(key, String.valueOf(limit), replaceWhenFull ? "1" : "0"), argv.toArray());
        if (resList.get(0) instanceof String && "f".equals(resList.get(0)))
            return null;
        Object[] result = new Object[2];
        result[0] = ((List) resList.get(0)).get(0);
        result[1] = resList.get(1);
        return result;
    }

    public static List<String> zRevRangeUseStartIdFirst(@NotNull StringRedisTemplate redisTemplate, String key, @NotNull QueryParam queryParam, boolean withScores) {
        String start = queryParam.getStartValue() == null ? String.valueOf(queryParam.getStartId()) : queryParam.getStartValue();
        return redisTemplate.execute(zRevRangeUseStartIdFirstScript, Collections.singletonList(key), start, String.valueOf(queryParam.getFrom()),
                String.valueOf(queryParam.getQueryNum()), withScores ? "1" : "0");
    }

    public static List<String> zRevRangeUseFromFirst(@NotNull StringRedisTemplate redisTemplate, String key, @NotNull QueryParam queryParam, boolean withScores) {
        String start = queryParam.getStartValue() == null ? String.valueOf(queryParam.getStartId()) : queryParam.getStartValue();
        return redisTemplate.execute(zRevRangeUseFromFirstScript, Collections.singletonList(key), String.valueOf(queryParam.getFrom()), start,
                String.valueOf(queryParam.getQueryNum()), withScores ? "1" : "0");
    }

    @Nullable
    public static List<ZSetOperations.TypedTuple<String>> readIndex(@NotNull StringRedisTemplate redisTemplate, String indexSetKey, @NotNull QueryParam queryParam, boolean startIdFirst, boolean withScore) {
        List<String> resList;
        if (startIdFirst)
            resList = RedisUtil.zRevRangeUseStartIdFirst(redisTemplate, indexSetKey, queryParam, withScore);
        else
            resList = RedisUtil.zRevRangeUseFromFirst(redisTemplate, indexSetKey, queryParam, withScore);
        if (resList == null || (resList.size() == 1 && resList.get(0) == null))
            return null;
        if (withScore) {
            List<ZSetOperations.TypedTuple<String>> tupleList = new ArrayList(resList.size() / 2);
            for (int i = 0; i < resList.size(); i += 2)
                tupleList.add(new DefaultTypedTuple(resList.get(i), Double.valueOf(resList.get(i + 1))));
            return tupleList;
        } else {
            List<ZSetOperations.TypedTuple<String>> tupleList = new ArrayList(resList.size());
            for (int i = 0; i < resList.size(); ++i)
                tupleList.add(new DefaultTypedTuple(resList.get(i), 0.0));
            return tupleList;
        }
    }

    public static double genScore(double value, double id) {
        int cnt = 0;
        while (id >= 1) {
            ++cnt;
            id /= 10;
        }
        id = (cnt + id) * (cnt > 9 ? 0.01 : 0.1);//id长度不可能是三位数，只考虑两位和一位数的情况
        return value + id;
    }

    public static long zRemRangeByLex(@NotNull StringRedisTemplate redisTemplate, String key, String min, String max) {
        return redisTemplate.execute(zRemRangeByLex, Collections.singletonList(key), min, max);
    }

    public static Set<String> scanAllKeys(@NotNull StringRedisTemplate redisTemplate, String pattern) {
        ScanOptions options = new ScanOptions.ScanOptionsBuilder().match(pattern).count(5000).build();
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            int failCnt = 0;
            do {
                try {
                    Set<String> keysTmp = new HashSet();
                    Cursor<byte[]> cursor = connection.scan(options);
                    while (cursor.hasNext())
                        keysTmp.add(new String(cursor.next()));
                    cursor.close();
                    return keysTmp;
                } catch (Exception e) {
                    ++failCnt;
                }
            } while (failCnt < 10);//IO异常重试十次
            log.warn("键值迭代失败，pattern:" + pattern);
            return null;
        });
        return keys;
    }

}
