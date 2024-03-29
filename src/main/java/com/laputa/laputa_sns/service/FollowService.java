package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.BaseService;
import com.laputa.laputa_sns.common.QueryParam;
import com.laputa.laputa_sns.common.RedisPrefix;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.dao.FollowDao;
import com.laputa.laputa_sns.executor.IndexExecutor;
import com.laputa.laputa_sns.model.entity.Follow;
import com.laputa.laputa_sns.model.entity.Notice;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.User;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.QueryTokenUtil;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.validator.FollowValidator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * 关注服务
 *
 * @author JQH
 * @since 下午 7:10 20/02/20
 */

@Slf4j
@EnableScheduling
@Service
public class FollowService extends BaseService<FollowDao, Follow> {

    private final int ADD = 0;
    private final int REMOVE = 1;
    private final int UPDATE = 2;

    private final int FOLLOWING = 1;
    private final int FOLLOWER = 2;
    private final UserService userService;
    private final NoticeService noticeService;
    private final IndexExecutor.CallBacks<Follow> indexExecutorCallBacks;
    private final FollowValidator followValidator;
    private final StringRedisTemplate redisTemplate;
    private String hmacKey;
    @Value("${timeout.redis.user.following-list}")
    private int redisTimeOut;

    /**
     * 用户最多关注100个人
     */
    @Value("${user-follow-max-num}")
    private int userFollowMaxNum;

    public FollowService(@Lazy UserService userService, CommonService commonService, NoticeService noticeService, FollowValidator followValidator, StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.noticeService = noticeService;
        this.followValidator = followValidator;
        this.redisTemplate = redisTemplate;
        this.indexExecutorCallBacks = initIndexExecutorCallBacks();
        this.hmacKey = CryptUtil.randUrlSafeStr(64, true);
    }

    @NotNull
    private String getFollowingListKey(Integer followerId) {
        return RedisPrefix.USER_FOLLOWING_LIST + ":" + followerId;
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @NotNull
    private String getFollowerZSetKey(Integer targetId) {
        return RedisPrefix.USER_FOLLOWER_LIST + ":" + targetId;
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @NotNull
    private String getFollowerZSetKey(String str) {
        return RedisPrefix.USER_FOLLOWER_LIST + ":" + str;
    }

    /**
     * 获取redis中保存的某个用户的关注列表字符串
     *
     * @param followerId
     * @return
     */
    @Nullable
    private List<String> getRedisFollowingStrList(Integer followerId) {
        String str = redisTemplate.opsForValue().get(getFollowingListKey(followerId));
        if (str == null) {
            return null;
        }
        return new ArrayList<>(Arrays.asList(str.split("-")));
    }

    /**
     * 获取redis中保存的某个用户的关注列表
     *
     * @param followerId
     * @return
     */
    @Nullable
    private List<Follow> getRedisFollowingList(Integer followerId) {
        // 获取关注列表字符串
        List<String> strList = getRedisFollowingStrList(followerId);
        if (strList == null) {
            return null;
        }
        List<Follow> followList = new ArrayList<>(strList.size());
        // 解析关注列表字符串
        for (int i = strList.size() - 1; i >= 0; --i) {
            String[] v = strList.get(i).split("_");
            followList.add(((Follow) new Follow().setTargetId(Integer.valueOf(v[0])).setType(Integer.valueOf(v[1]))));
        }
        return followList;
    }

    /**
     * 设置redis中的关注列表，redis中的关注列表直接使用字符串存储
     * 因为获取用户的动态数量时需要取出用户的全部关注
     * 所以取出关注列表是个频繁操作，直接使用一个字符串存储会更快
     *
     * @param followerId
     * @param followingList 关注列表，可能是String类型，也可能是Follow类型
     * @param listClass     用于指明followingList参数的类型
     */
    private void setRedisFollowingList(Integer followerId, @NotNull List<?> followingList, @NotNull Class<?> listClass) {
        if (followingList.size() == 0) {
            return;
        }
        if (listClass.equals(Follow.class)) {
            List<String> strList = new ArrayList<>(followingList.size());
            for (int i = 0; i < followingList.size(); ++i) {
                Follow follow = (Follow) followingList.get(i);
                strList.add(follow.getTargetId() + "_" + follow.getType());
            }
            followingList = strList;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(followingList.get(0));
        for (int i = 1; i < followingList.size(); ++i) {
            stringBuilder.append("-").append(followingList.get(i));
        }
        redisTemplate.opsForValue().set(getFollowingListKey(followerId), stringBuilder.toString(), redisTimeOut, TimeUnit.MINUTES);
    }

    /**
     * 获取字符串列表中的目标ID所能匹配上的字符串所在的位置
     *
     * @param targetId
     * @param list
     * @return
     */
    private int getTargetIdxOfList(Integer targetId, @NotNull List<String> list) {
        if (list == null) {
            return -1;
        }
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i).split("_")[0].equals(String.valueOf(targetId))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 操作redis中的关注列表，包括移除、更新、添加
     *
     * @param follow
     * @param list
     * @param type
     * @return
     */
    private boolean opRedisFollowingList(@NotNull Follow follow, List<String> list, int type) {
        String value = follow.getTargetId() + "_" + follow.getType();
        if (list == null) {
            list = getRedisFollowingStrList(follow.getFollowerId());
        }
        if (list == null) {
            return false;
        }
        int idx = getTargetIdxOfList(follow.getTargetId(), list);
        if (idx != -1) {
            if (type == REMOVE) {
                list.remove(idx);
            } else if (type == UPDATE) {
                list.set(idx, value);
            }
            setRedisFollowingList(follow.getFollowerId(), list, String.class);
        } else if (idx == -1 && type == ADD) {
            list.add(value);
            setRedisFollowingList(follow.getFollowerId(), list, String.class);
        } else {
            return false;
        }
        return true;
    }

    /**
     * 给关注实体对象设置用户信息
     *
     * @param followList
     * @param type
     * @return
     */
    @SneakyThrows
    private Result<Object> fillFollowWithUser(@NotNull List<Follow> followList, int type) {
        if (type == FOLLOWING) {
            return userService.multiSetUser(followList, Follow.class.getMethod("getTargetId"), Follow.class.getMethod("setTarget", User.class));
        } else if (type == FOLLOWER) {
            return userService.multiSetUser(followList, Follow.class.getMethod("getFollowerId"), Follow.class.getMethod("setFollower", User.class));
        }
        return new Result<Object>(Result.FAIL);
    }

    public Result<List<Follow>> readFollowingList(Integer userId, boolean withUserInfo, @NotNull Operator operator) {
        if (operator.getFollowList() != null && operator.getUserId().equals(userId)) {
            if (withUserInfo) {
                fillFollowWithUser(operator.getFollowList(), FOLLOWING);
            }
            return new Result<List<Follow>>(SUCCESS).setObject(operator.getFollowList());
        }
        Result<User> userResult = userService.readUserWithCounter(userId, operator);
        if (userResult.getState() == FAIL) {
            return new Result<List<Follow>>(userResult);
        }
        Long cnt = userResult.getObject().getFollowingCnt();
        if (cnt == null || cnt == 0) {
            return new Result<List<Follow>>(SUCCESS).setObject(new ArrayList<>(0));
        }
        List<Follow> followList = getRedisFollowingList(userId);
        // redis中没有
        if (followList == null) {
            Follow queryEntity = new Follow().setFollowerId(userId);
            queryEntity.setQueryParam(new QueryParam().setCustomAddition("FOLLOWING"));
            // 获取following要设置follower的id
            followList = selectList(queryEntity);
            if (followList == null) {
                return new Result<List<Follow>>(FAIL).setErrorCode(1010040101).setMessage("数据库操作失败");
            }
            setRedisFollowingList(userId, followList, Follow.class);
        }
        if (withUserInfo && followList != null && followList.size() != 0) {
            fillFollowWithUser(followList, FOLLOWING);
        }
        return new Result<List<Follow>>(SUCCESS).setObject(followList);
    }

    /**
     * 向redis中添加一条关注者记录(即某个用户关注了某个用户)
     * redis中用户的关注者列表是存在ZSet中的，而不像关注列表存的是一个字符串
     *
     * @param follow
     */
    private void addNewToRedisFollowerIndex(@NotNull Follow follow) {
        redisTemplate.opsForZSet().add(getFollowerZSetKey(follow.getTargetId()), getRedisValue(follow.getFollowerId(), null), System.currentTimeMillis());
    }

    /**
     * 从redis的关注者列表中移除一个对象
     *
     * @param follow
     */
    private void removeRedisFollowerIndex(@NotNull Follow follow) {
        String key = getFollowerZSetKey(follow.getTargetId());
        long res = redisTemplate.opsForZSet().remove(key, getRedisValue(follow.getFollowerId(), null));
        // 该follow记录对应的zSet中的value包含followId，由于不知道followId的值，所以采用字典序范围删除
        if (res == 0) {
            String min = "[" + follow.getFollowerId() + ":0";
            String max = "[" + follow.getFollowerId() + ":9";
            RedisUtil.zRemRangeByLex(redisTemplate, key, min, max);
        }
    }

    private String getRedisValue(Integer followerId, Integer followId) {
        if (followId == null) {
            return String.valueOf(followerId);
        }
        return followerId + ":" + followId;
    }

    @NotNull
    private RedisValue parseRedisValue(@NotNull String v) {
        String[] s = v.split(":");
        if (s.length == 1) {
            return new RedisValue(Integer.valueOf(s[0]), null);
        }
        return new RedisValue(Integer.valueOf(s[0]), Integer.valueOf(s[1]));
    }

    @NotNull
    private IndexExecutor.CallBacks<Follow> initIndexExecutorCallBacks() {
        IndexExecutor.CallBacks<Follow> callBacks = new IndexExecutor.CallBacks<Follow>();
        callBacks.getIdListCallBack = (executor) -> {
            IndexExecutor<Follow>.Param param = executor.param;
            int targetId = param.paramEntity.getTargetId();
            List<TypedTuple<String>> indexList = RedisUtil.readIndex(redisTemplate, getFollowerZSetKey(targetId), param.paramEntity.getQueryParam(), false, true);
            List<Integer> idList = null;
            if (indexList != null) {
                idList = new ArrayList<>(indexList.size());
                for (int i = 0; i < indexList.size(); ++i) {
                    idList.add(parseRedisValue(indexList.get(i).getValue()).followerId);
                }
                if (indexList.size() != 0) {
                    param.newStartValue = indexList.get(indexList.size() - 1).getValue();
                    param.newStartId = parseRedisValue(param.newStartValue).followId;
                }
            }
            executor.param.indexSetList = indexList;
            param.idList = idList;
        };
        callBacks.multiReadEntityCallBack = (executor) -> {
            IndexExecutor<Follow>.Param param = executor.param;
            Result<List<User>> userListResult = userService.multiReadUser(param.idList);
            if (userListResult.getState() == FAIL) {
                return new Result<List<Follow>>(userListResult);
            }
            List<User> userList = userListResult.getObject();
            List<Follow> followList = new ArrayList<>(userList.size());
            for (int i = 0; i < userList.size(); ++i) {
                Date createTime = new Date(Math.round(param.indexSetList.get(i).getScore()));
                followList.add((Follow) new Follow().setFollower(userList.get(i)).setCreateTime(createTime));
            }
            return new Result<List<Follow>>(SUCCESS).setObject(followList);
        };
        callBacks.getDbListCallBack = (executor) -> {
            Follow follow = executor.param.paramEntity;
            if (follow.getQueryParam().getStartId() == null || follow.getQueryParam().getStartId().equals(0)) {
                RedisValue redisValue = parseRedisValue(follow.getQueryParam().getStartValue());
                if (redisValue.followId != null) {
                    follow.getQueryParam().setStartId(redisValue.followId).setUseStartIdAtSql(1);
                } else {
                    follow.setFollowerId(redisValue.followerId).getQueryParam().setUseStartIdAtSql(0);
                }
            } else {
                follow.getQueryParam().setUseStartIdAtSql(1);
            }
            List<Follow> followList = selectList(follow);
            if (followList == null) {
                return new Result<List<Follow>>(FAIL).setErrorCode(1010040102).setMessage("数据库操作失败");
            }
            fillFollowWithUser(followList, FOLLOWER);
            return new Result<List<Follow>>(SUCCESS).setObject(followList);
        };
        callBacks.multiSetRedisIndexCallBack = (entityList, executor) -> {
            Set<Tuple> indexSet = new HashSet<>();
            for (int i = 0; i < entityList.size(); ++i) {
                Follow follow = entityList.get(i);
                Integer followId = i == entityList.size() - 1 ? follow.getId() : null;
                indexSet.add(new DefaultTuple(getRedisValue(follow.getFollowerId(), followId).getBytes(), (double) follow.getCreateTime().getTime()));
            }
            Object[] result = RedisUtil.addToZSetAndGetLastAndLength(redisTemplate, getFollowerZSetKey(executor.param.paramEntity.getTargetId()), indexSet, Integer.MAX_VALUE, false);
            if (result == null) {
                return;
            }
            executor.param.newStartValue = (String) result[0];
            executor.param.newFrom = (int) (long) result[1];
            executor.param.newStartId = entityList.get(entityList.size() - 1).getId();
        };
        return callBacks;
    }

    public Result<List<Follow>> readFollowerList(@NotNull Follow follow, Operator operator) {
        if (!follow.isValidReadIndexOfTargetParam()) {
            return new Result<List<Follow>>(FAIL).setErrorCode(1010040216).setMessage("操作错误，参数不合法");
        }
        Result<Object> validateTokenResult = QueryTokenUtil.validateTokenAndSetQueryParam(follow, 0, hmacKey);
        if (validateTokenResult.getState() == FAIL) {
            return new Result<List<Follow>>(validateTokenResult);
        }
        Result<User> userResult = userService.readUserWithCounter(follow.getTargetId(), operator);
        if (userResult.getState() == FAIL) {
            return new Result<List<Follow>>(userResult);
        }
        Follow queryEntity = (Follow) new Follow().setQueryParam(new QueryParam());
        follow.getQueryParam().setCustomAddition("FOLLOWER");
        IndexExecutor<Follow> indexExecutor = new IndexExecutor<>(follow, queryEntity, null, indexExecutorCallBacks, operator);
        indexExecutor.param.childNumOfParent = userResult.getObject().getFollowersCnt();
        indexExecutor.param.initStartId = Integer.MAX_VALUE;
        Result<List<Follow>> followListResult = indexExecutor.doIndex();
        if (followListResult.getState() == FAIL) {
            return followListResult;
        }
        String newToken = QueryTokenUtil.generateQueryToken(follow, followListResult.getObject(), queryEntity.getQueryParam(), 0, hmacKey);
        return followListResult.setAttachedToken(newToken);
    }

    public Result<Integer> createFollow(@NotNull Follow follow, Operator operator) {
        if (!follow.isValidInsertParam()) {
            return new Result<Integer>(FAIL).setErrorCode(1010040206).setMessage("操作错误，参数不合法");
        }
        if (!followValidator.checkCreatePermission(follow, operator)) {
            return new Result<Integer>(FAIL).setErrorCode(1010040209).setMessage("操作失败，权限错误");
        }
        if (!userService.existUser(follow.getTargetId())) {
            return new Result<Integer>(FAIL).setErrorCode(1010040213).setMessage("目标用户不存在");
        }
        if (follow.getTargetId().equals(operator.getUserId())) {
            return new Result<Integer>(FAIL).setErrorCode(1010040215).setMessage("不能关注自己");
        }
        follow.setFollowerId(operator.getUserId());
        List<String> followingStrList = null;
        if (operator.getUser().getFollowingCnt() > 0) {
            followingStrList = getRedisFollowingStrList(follow.getFollowerId());
            if (followingStrList != null && followingStrList.size() >= userFollowMaxNum) {
                return new Result<Integer>(FAIL).setErrorCode(1010040214).setMessage("最多只能关注" + userFollowMaxNum + "个用户");
            }
            if (followingStrList != null && getTargetIdxOfList(follow.getTargetId(), followingStrList) != -1) {
                return new Result<Integer>(FAIL).setErrorCode(1010040212).setMessage("操作错误，不能重复关注");
            }
        }
        int res = insertOne(follow);
        if (res == -1) {
            return new Result<Integer>(FAIL).setErrorCode(1010040103).setMessage("数据库操作失败");
        }
        // Redis中有该用户的关注列表
        if (followingStrList != null) {
            opRedisFollowingList(follow, followingStrList, ADD);
        }
        addNewToRedisFollowerIndex(follow);
        userService.updateFollowingCnt(follow.getFollowerId(), 1L);
        userService.updateFollowersCnt(follow.getTargetId(), 1L);
        noticeService.pushNotice(follow.getTargetId(), Notice.TYPE_FOLLOWER, follow.getTargetId());
        return new Result<Integer>(SUCCESS).setObject(follow.getId());
    }

    public Result<Object> updateFollow(@NotNull Follow follow, Operator operator) {
        if (!follow.isValidUpdateParam()) {
            return new Result<Object>(FAIL).setErrorCode(1010040207).setMessage("操作错误，参数不合法");
        }
        if (!followValidator.checkUpdatePermission(follow, operator)) {
            return new Result<Object>(FAIL).setErrorCode(1010040210).setMessage("操作失败，权限错误");
        }
        if (!userService.existUser(follow.getTargetId())) {
            return new Result<Object>(FAIL).setErrorCode(1010040213).setMessage("目标用户不存在");
        }
        follow.setFollowerId(operator.getUserId());
        if (updateOne(follow) == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010040105).setMessage("数据库操作失败");
        }
        opRedisFollowingList(follow, null, UPDATE);
        return new Result<Object>(Result.SUCCESS);
    }

    public Result<Object> deleteFollow(@NotNull Follow follow, Operator operator) {
        if (!follow.isValidDeleteParam()) {
            return new Result<Object>(FAIL).setErrorCode(1010040208).setMessage("操作错误，参数不合法");
        }
        if (!followValidator.checkDeletePermission(follow, operator)) {
            return new Result<Object>(FAIL).setErrorCode(1010040211).setMessage("操作失败，权限错误");
        }
        if (!userService.existUser(follow.getTargetId())) {
            return new Result<Object>(FAIL).setErrorCode(1010040213).setMessage("目标用户不存在");
        }
        follow.setFollowerId(operator.getUserId());
        if (deleteOne(follow) == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010040104).setMessage("数据库操作失败");
        }
        opRedisFollowingList(follow, null, REMOVE);
        removeRedisFollowerIndex(follow);
        userService.updateFollowingCnt(follow.getFollowerId(), -1L);
        userService.updateFollowersCnt(follow.getTargetId(), -1L);
        return new Result<Object>(Result.SUCCESS);
    }

    @Scheduled(cron = "0 10 3 * * ?")
    public void dailyFlushRedisToDb() {
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, getFollowerZSetKey("*")));
        log.info("粉丝列表索引清除");
        hmacKey = CryptUtil.randUrlSafeStr(64, true);
    }

    /**
     * 封装的redis中一条关注记录的属性
     */
    private class RedisValue {
        /**
         * 数据库中关注表的主键，无业务含义
         */
        Integer followId;
        /**
         * 关注者ID
         */
        Integer followerId;

        RedisValue(Integer followerId, Integer followId) {
            this.followId = followId;
            this.followerId = followerId;
        }
    }

}
