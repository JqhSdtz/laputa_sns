package com.laputa.laputa_sns.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import com.laputa.laputa_sns.common.TmpEntry;
import com.laputa.laputa_sns.model.entity.AbstractContent;
import com.laputa.laputa_sns.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author JQH
 * @since 下午 3:14 20/03/13
 */

@Slf4j
public class RedisHelper<T extends AbstractBaseEntity> {

    private final StringRedisTemplate redisTemplate;

    private final Class<T> entityType;

    private final Random random = new Random();

    private final ObjectMapper basicMapper;
    private final String basicPrefix;
    private final Integer basicTimeout;

    private final ObjectMapper contentMapper;
    private final String contentPrefix;
    private final Integer contentTimeout;

    private final String counterPrefix;

    private final int refreshProbability = 100;//1%的概率刷新key

    /**
     * 只包含计数器
     */
    public RedisHelper(String counterPrefix, StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.counterPrefix = counterPrefix;
        this.basicPrefix = null;
        this.basicMapper = null;
        this.basicTimeout = null;
        this.contentPrefix = null;
        this.contentMapper = null;
        this.contentTimeout = null;
        this.entityType = null;
    }

    /**
     * 只包含基础数据
     */
    public RedisHelper(String basicPrefix, ObjectMapper basicMapper, Integer basicTimeout, Class<T> entityType, StringRedisTemplate redisTemplate) {
        this.basicPrefix = basicPrefix;
        this.basicMapper = basicMapper;
        this.basicTimeout = basicTimeout;
        this.contentPrefix = null;
        this.contentMapper = null;
        this.contentTimeout = null;
        this.counterPrefix = null;
        this.entityType = entityType;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 包含基础数据，内容数据以及计数器
     */
    public RedisHelper(String basicPrefix, ObjectMapper basicMapper, Integer basicTimeout, String contentPrefix, ObjectMapper contentMapper, Integer contentTimeout, String counterPrefix, Class<T> entityType, StringRedisTemplate redisTemplate) {
        this.basicPrefix = basicPrefix;
        this.basicMapper = basicMapper;
        this.basicTimeout = basicTimeout;
        this.contentPrefix = contentPrefix;
        this.contentMapper = contentMapper;
        this.contentTimeout = contentTimeout;
        this.counterPrefix = counterPrefix;
        this.entityType = entityType;
        this.redisTemplate = redisTemplate;
    }

    @NotNull
    public String getBasicKey(Integer id) {
        return basicPrefix + ":" + id;
    }

    @NotNull
    public String getBasicKey(String str) {
        return basicPrefix + ":" + str;
    }

    @NotNull
    public String getContentKey(Integer id) {
        return contentPrefix == null ? null : contentPrefix + ":" + id;
    }

    @NotNull
    public String getContentKey(String str) {
        return contentPrefix == null ? null : contentPrefix + ":" + str;
    }

    @NotNull
    public String getCounterKey(Integer id) {
        return counterPrefix == null ? null : counterPrefix + ":" + id;
    }

    @NotNull
    public String getCounterKey(String str) {
        return counterPrefix == null ? null : counterPrefix + ":" + str;
    }

    @Nullable
    public Long[] getRedisCounterCnt(@NotNull Integer id, Object... hks) {
        String key = getCounterKey(id);
        if (key == null)
            return null;
        List<Object> strValue = redisTemplate.opsForHash().multiGet(key, Arrays.asList(hks));
        if (strValue == null)
            return null;
        Long[] v = new Long[hks.length];
        for (int i = 0; i < hks.length; ++i)
            if (strValue.get(i) != null)
                v[i] = Long.valueOf((String) strValue.get(i));
        return v;
    }

    public List<List<String>> multiGetRedisCounterCnt(@NotNull List<T> entityList, String... hks) {
        if (counterPrefix == null)
            return null;
        List<String> counterKeys = new ArrayList<>(entityList.size());
        for (int i = 0; i < entityList.size(); ++i) {
            Integer id = entityList.get(i) == null ? -1 : entityList.get(i).getId();
            counterKeys.add(getCounterKey(id));
        }
        return multiGetRedisCounterCntByKeys(counterKeys, false, hks);
    }

    @SuppressWarnings("unchecked")
    public List<List<String>> multiGetRedisCounterCntByKeys(@NotNull List<String> counterKeys, boolean delAfterGet, String... hks) {
        if (counterPrefix == null)
            return null;
        byte[][] rawHks = new byte[hks.length][];
        for (int i = 0; i < hks.length; ++i)
            rawHks[i] = hks[i].getBytes();
        List<Object> resList = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            for (int i = 0; i < counterKeys.size(); ++i) {
                byte[] rKey = counterKeys.get(i).getBytes();
                connection.hMGet(rKey, rawHks);
                if (delAfterGet)
                    connection.del(rKey);
            }
            return null;
        });
        if (delAfterGet) {
            List<List<String>> strList = new ArrayList<>(resList.size() / 2);
            for (int i = 0; i < resList.size(); ++i)
                if (resList.get(i) instanceof List)
                    strList.add((List<String>) resList.get(i));
            return strList;
        }
        return (List<List<String>>) (List<?>) resList;
    }

    public void updateCounters(Integer id, Map<String, Long> fieldsMap) {
        String key = getCounterKey(id);
        if (key == null || fieldsMap == null || fieldsMap.size() == 0)
            return;
        redisTemplate.execute(connection -> {
            byte[] rawKey = key.getBytes();
            for (Map.Entry<String, Long> entry : fieldsMap.entrySet())
                connection.hIncrBy(rawKey, entry.getKey().getBytes(), entry.getValue());
            return null;
        }, false, true);
    }

    public void updateCounter(Integer id, String hk, Long delta) {
        redisTemplate.opsForHash().increment(getCounterKey(id), hk, delta);
    }

    public void multiUpdateCounter(List<Integer> idList, String hk, Long delta) {
        redisTemplate.execute(connection -> {
            byte[] rawHk = hk.getBytes();
            for (int i = 0; i < idList.size(); ++i)
                connection.hIncrBy(getCounterKey(idList.get(i)).getBytes(), rawHk, delta);
            return null;
        }, false, true);
    }

    public boolean existEntity(Integer id) {
        return redisTemplate.hasKey(getBasicKey(id));
    }

    private String getValue(String key, boolean withRefresh, int timeOut) {
        if (!withRefresh)
            return redisTemplate.opsForValue().get(key);
        List<Object> resList = redisTemplate.executePipelined((RedisCallback<String>) connection -> {
            byte[] rawKey = key.getBytes();
            connection.get(rawKey);
            if (random.nextInt() % refreshProbability == 0)
                connection.expire(rawKey, timeOut * 60);
            return null;
        });
        return (String) resList.get(0);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public T getEntity(Integer id, boolean isFull, boolean withRefresh) {
        if (contentPrefix == null)
            isFull = false;
        String value = getValue(getBasicKey(id), withRefresh, basicTimeout);
        if (value == null)
            return null;
        String contentValue = null;
        if (isFull) {
            contentValue = getValue(getContentKey(id), withRefresh, contentTimeout);
            if (contentValue == null)
                return null;
        }
        try {
            T entity = basicMapper.readValue(value, entityType);
            if (isFull) {
                T content = contentMapper.readValue(contentValue, entityType);
                if (content instanceof AbstractContent)
                    ((AbstractContent<T>) entity).setEntityContent(content);
            }
            entity.setId(id);
            return entity;
        } catch (JsonProcessingException e) {
            log.warn("Redis json 解析错误 " + e.getMessage());
            return null;
        }
    }

    public void setEntity(@NotNull T entity, boolean isFull) {
        if (contentPrefix == null)
            isFull = false;
        try {
            redisTemplate.opsForValue().set(getBasicKey(entity.getId()), basicMapper.writeValueAsString(entity),
                    basicTimeout, TimeUnit.MINUTES);
            if (isFull)
                redisTemplate.opsForValue().set(getContentKey(entity.getId()), contentMapper.writeValueAsString(entity),
                        contentTimeout, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("Redis json 写入错误 " + e.getMessage());
        }
    }

    public void removeEntity(Integer id) {
        if (contentPrefix == null)
            redisTemplate.delete(getBasicKey(id));
        else
            redisTemplate.delete(Arrays.asList(getBasicKey(id), getContentKey(id)));
    }

    public void multiSetAndRefreshEntity(List<T> entities, HashMap<Integer, T> newEntityMap, boolean isFull) {
        redisTemplate.execute(connection -> {
            try {
                for (int i = 0; i < entities.size(); ++i) {
                    T entity = entities.get(i);
                    if (entity == null)
                        continue;
                    boolean isNew = newEntityMap == null ? true : newEntityMap.containsKey(entity.getId());
                    boolean isRefresh = random.nextInt() % refreshProbability == 0;
                    byte[] basicKey = getBasicKey(entity.getId()).getBytes();
                    if (isNew) {
                        byte[] basicValue = basicMapper.writeValueAsString(entity).getBytes();
                        connection.set(basicKey, basicValue, Expiration.seconds(basicTimeout * 60), RedisStringCommands.SetOption.UPSERT);
                    }
                    if (isRefresh)
                        connection.expire(basicKey, basicTimeout * 60);
                    if (contentPrefix != null && isFull) {
                        byte[] contentKey = getContentKey(entity.getId()).getBytes();
                        if (isNew) {
                            byte[] contentValue = contentMapper.writeValueAsString(entity).getBytes();
                            connection.set(contentKey, contentValue, Expiration.seconds(contentTimeout * 60), RedisStringCommands.SetOption.UPSERT);
                        }
                        if (isRefresh)
                            connection.expire(contentKey, contentTimeout * 60);
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("Redis json 写入错误 " + e.getMessage());
            }
            return null;
        }, false, true);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public List<T> multiGetEntity(@NotNull List<Integer> idList, boolean isFull) {
        if (contentPrefix == null)
            isFull = false;
        int keysLen = idList.size();
        List<String> contentKeys;
        List<String> basicKeys = new ArrayList<>(keysLen);
        List<T> entityList = new ArrayList<>(keysLen);
        try {
            if (isFull) {//MGET操作返回的List中，没有的key返回null，所以返回的List和keyList是一一对应的
                //大多数情况下是有content没有basic，所以先获取content，有content的再获取basic
                contentKeys = new ArrayList<>(keysLen);
                for (int i = 0; i < idList.size(); ++i)
                    contentKeys.add(getContentKey(idList.get(i)));
                List<String> contentStrList = redisTemplate.opsForValue().multiGet(contentKeys);
                List<T> contentList = new ArrayList<>(keysLen);
                for (int i = 0; i < keysLen; ++i)
                    if (contentStrList.get(i) != null) {
                        contentList.add(contentMapper.readValue(contentStrList.get(i), entityType));
                        basicKeys.add(getBasicKey(idList.get(i)));//有这个content，才去取basic
                    } else
                        contentList.add(null);
                List<String> basicStrList = redisTemplate.opsForValue().multiGet(basicKeys);
                for (int i = 0, j = 0; i < keysLen; ++i)//contentList和contentStrList对应
                    if (contentList.get(i) == null) {//content为空，一定没有basic
                        entityList.add(null);//加一个空值占位
                    } else if (j < basicStrList.size()) {
                        if (basicStrList.get(j) != null) {//content和basic都有，才最终加入entityList
                            T basic = basicMapper.readValue(basicStrList.get(j), entityType);
                            if (contentList.get(i) instanceof AbstractContent)
                                ((T) ((AbstractContent<T>) basic).setEntityContent(contentList.get(i))).setId(idList.get(i));
                            entityList.add(basic);
                        } else//否则，还是加空
                            entityList.add(null);
                        ++j;
                    }
            } else {//直接获取basic即可
                for (int i = 0; i < keysLen; ++i)
                    basicKeys.add(getBasicKey(idList.get(i)));
                List<String> basicStrList = redisTemplate.opsForValue().multiGet(basicKeys);
                for (int i = 0; i < keysLen; ++i)
                    if (basicStrList.get(i) != null)
                        entityList.add((T) basicMapper.readValue(basicStrList.get(i), entityType).setId(idList.get(i)));
                    else
                        entityList.add(null);
            }
        } catch (JsonProcessingException e) {
            log.warn("Redis json 解析错误 " + e.getMessage());
            return null;
        }
        return entityList;
    }

    @SuppressWarnings("unchecked")
    public List<TmpEntry>[] flushRedisCounter(String... hks) {
        if (counterPrefix == null)
            return null;
        Set<String> counterKeys = RedisUtil.scanAllKeys(redisTemplate, getCounterKey("*"));
        List<TmpEntry>[] cntLists = new List[hks.length];
        for (int i = 0; i < hks.length; ++i)
            cntLists[i] = new ArrayList<>(counterKeys.size());
        List<String> counterKeyList = new ArrayList<>(counterKeys);
        List<List<String>> counterList = multiGetRedisCounterCntByKeys(counterKeyList, true, hks);
        for (int i = 0; i < counterList.size(); ++i) {
            Integer entityId = Integer.valueOf(counterKeyList.get(i).split(":")[1]);
            List<String> vList = counterList.get(i);
            for (int j = 0; j < hks.length; ++j)
                if (vList.get(j) != null)
                    cntLists[j].add(new TmpEntry(entityId, Integer.valueOf(vList.get(j))));
        }
        return cntLists;
    }

}
