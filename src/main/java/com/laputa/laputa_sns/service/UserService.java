package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.annotation.NeedLogin;
import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.dao.UserDao;
import com.laputa.laputa_sns.helper.QueryHelper;
import com.laputa.laputa_sns.helper.RedisHelper;
import com.laputa.laputa_sns.model.entity.*;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.validator.UserValidator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * 用户服务，包括用户的增加、修改等
 *
 * @author JQH
 * @since 下午 11:09 20/02/12
 */

@Slf4j
@Service
@EnableScheduling
@Order(2)
public class UserService extends BaseService<UserDao, User> implements ApplicationRunner {

    private final CommonService commonService;
    private final FollowService followService;
    private final PostService postService;
    private final CategoryService categoryService;
    private final OperatorService operatorService;
    private final UserValidator userValidator;
    private final RedisHelper<User> redisHelper;
    private final QueryHelper<User> queryHelper;
    private final RedisHelper<UserRecvSetting> recvSettingRedisHelper;
    private final QueryHelper<UserRecvSetting> recvSettingQueryHelper;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> pushRecentVisitCategoryScript = new DefaultRedisScript<>(
            "local zscore = redis.call('zscore', KEYS[1], ARGV[1])\n" + "if (zscore ~= nil and zscore ~= false and tonumber(zscore) > 9000000000000) then\n" +
                    "\treturn 0 \n" + "end\n" + "redis.call('zadd', KEYS[1], ARGV[2], ARGV[1])\n" + "local len = redis.call('zcard', KEYS[1])\n" + "if (len > tonumber(ARGV[3])) then\n" +
                    "\tredis.call('zremrangebyrank', KEYS[1], 0, 0)\n" + "end\n" + "return len", Long.class);
    private final DefaultRedisScript<Long> pinRecentVisitCategoryScript = new DefaultRedisScript<>(
            "if (ARGV[3] == '0') then\n" + "\treturn redis.call('zadd', KEYS[1], ARGV[2], ARGV[1])\n" + "else\n" +
                    "\tlocal maxScore = redis.call('zrangebyscore', KEYS[1], 9000000000000, 9999999999999, 'WITHSCORES', 'LIMIT', 0, 1)\n" +
                    "\tlocal setScore = 9999999999999\n" + "\tif (#maxScore ~= 0) then\n" + "\t\tsetScore = tonumber(maxScore[2]) - 1\n" + "\tend\n" +
                    "\treturn redis.call('zadd', KEYS[1], setScore, ARGV[1])\n" + "end", Long.class);
    /**
     * 记录用户最近访问的15个目录
     */
    @Value("${user-recent-visit-category-num}")
    private int userRecentVisitCategoryNum;
    /**
     * 在@操作时根据用户名匹配到的用户数限制
     */
    @Value("${user-name-match-limit}")
    private int userNameMatchLimit;

    public UserService(UserValidator userValidator, @NotNull StringRedisTemplate redisTemplate, @NotNull Environment environment, CommonService commonService, FollowService followService, PostService postService, CategoryService categoryService, OperatorService operatorService) {
        this.redisTemplate = redisTemplate;
        this.userValidator = userValidator;
        this.commonService = commonService;
        this.followService = followService;
        this.postService = postService;
        this.categoryService = categoryService;
        this.operatorService = operatorService;
        ObjectMapper basicMapper = new ObjectMapper();
        basicMapper.setFilterProvider(new SimpleFilterProvider().addFilter("UserFilter", SimpleBeanPropertyFilter
                .filterOutAllExcept("nick_name", "raw_avatar", "type", "state", "talk_ban_to", "top_post_id", "followers_cnt", "following_cnt", "post_cnt")));
        int basicTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.user.basic"));
        this.redisHelper = new RedisHelper<>(RedisPrefix.USER_BASIC, basicMapper, basicTimeOut, null, null, null, RedisPrefix.USER_COUNTER, User.class, redisTemplate);
        this.queryHelper = new QueryHelper<>(this, redisHelper);
        ObjectMapper recvSettingMapper = new ObjectMapper();
        recvSettingMapper.setFilterProvider(new SimpleFilterProvider().addFilter("UserRecvSettingFilter", SimpleBeanPropertyFilter
                .filterOutAllExcept("recv_like", "recv_email", "recv_wx")));
        int recvSettingTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.user.recv-setting"));
        this.recvSettingRedisHelper = new RedisHelper<>(RedisPrefix.USER_RECV_SETTING, recvSettingMapper, recvSettingTimeOut, UserRecvSetting.class, redisTemplate);
        this.recvSettingQueryHelper = new QueryHelper<>(null, recvSettingRedisHelper);
        this.recvSettingQueryHelper.selectOneCallBack = param -> dao.selectRecvSetting(param.getId());
    }

    private int updateTopPost(int userId, Integer postId) {
        return dao.updateTopPost(userId, postId);
    }

    private int updateRecvSetting(UserRecvSetting recvSetting) {
        return dao.updateRecvSetting(recvSetting);
    }

    private int updatePassword(User user) {
        return dao.updatePassword(user);
    }

    @Override
    /**加载全部用户*/
    public void run(ApplicationArguments args) {
        executeLoadAllUser();
    }

    private void executeLoadAllUser() {
        String key = RedisPrefix.USER_NAME_IDX_HASH;
        // Redis中已经存在用户名称和ID的映射
        if (redisTemplate.hasKey(key)) {
            Globals.UserNameInitialized = true;
            log.info("已存在全部用户名");
            return;
        }
        User queryEntity = new User();
        int queryNum = 5000;
        queryEntity.setQueryParam(new QueryParam().setQueryType(QueryParam.SPEC1).setQueryNum(queryNum)
                .setIsPaged(1).setOrderBy("id").setOrderDir("asc").setStartId(-1).setUseStartIdAtSql(1));
        int from = 0;
        List<User> userList;
        while (true) {
            queryEntity.getQueryParam().setFrom(from);
            userList = selectList(queryEntity);
            if (userList == null || userList.size() == 0) {
                break;
            }
            HashMap<String, String> map = new HashMap<>(userList.size());
            for (int i = 0; i < userList.size(); ++i) {
                map.put(userList.get(i).getNickName(), String.valueOf(userList.get(i).getId()));
            }
            redisTemplate.opsForHash().putAll(key, map);
            from += queryNum;
            queryEntity.getQueryParam().setStartId(userList.get(userList.size() - 1).getId());
            //TimeUnit.SECONDS.sleep(1);
        }
        log.info("已加载全部用户名");
        Globals.UserNameInitialized = true;
    }

    public Result<Object> checkNickName(String nickName) {
        if (redisTemplate.opsForHash().hasKey(RedisPrefix.USER_NAME_IDX_HASH, nickName)) {
            return new Result<Object>(FAIL).setErrorCode(1010020201).setMessage("用户名已存在");
        }
        return new Result<Object>(Result.SUCCESS);
    }

    public Integer getUserIdByName(String nickName) {
        String str = (String) redisTemplate.opsForHash().get(RedisPrefix.USER_NAME_IDX_HASH, nickName);
        return str == null ? null : Integer.valueOf(str);
    }

    public List<Map.Entry<Object, Object>> getUserIdByNamePattern(String pattern) {
        ScanOptions opt = new ScanOptions.ScanOptionsBuilder().match(pattern).count(userNameMatchLimit).build();
        List<Map.Entry<Object, Object>> resultList = new ArrayList<>();
        try {
            Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(RedisPrefix.USER_NAME_IDX_HASH, opt);
            // hscan的count属性只是一个提示值，并不保证返回的就是count的数值
            int cnt = 0;
            while (cursor.hasNext() && cnt < userNameMatchLimit) {
                resultList.add(cursor.next());
                ++cnt;
            }
            cursor.close();
        } catch (IOException e) {
            log.warn("用户模糊查询失败，pattern:" + pattern);
            e.printStackTrace();
        }
        return resultList;
    }

    private void addNickNameIdx(Integer id, String nickName) {
        redisTemplate.opsForHash().put(RedisPrefix.USER_NAME_IDX_HASH, nickName, String.valueOf(id));
    }

    private void alterNickNameIdx(Integer id, @NotNull String oriName, @NotNull String newName) {
        byte[] key = RedisPrefix.USER_NAME_IDX_HASH.getBytes();
        byte[] oriHk = oriName.getBytes();
        byte[] newHk = newName.getBytes();
        byte[] value = String.valueOf(id).getBytes();
        redisTemplate.execute(connection -> {
            connection.hDel(key, oriHk);
            connection.hSet(key, newHk, value);
            return null;
        }, false, true);
    }

    private String getRecentVisitCategoryKey(Integer userId) {
        return RedisPrefix.USER_RECENT_VISIT_CATEGORY + ":" + userId;
    }

    private String getLastRefreshTimeKey() {
        return RedisPrefix.USER_LAST_REFRESH_TIME;
    }

    private String getRecentVisitCategoryKey(String str) {
        return RedisPrefix.USER_RECENT_VISIT_CATEGORY + ":" + str;
    }

    public long pushUserRecentVisitCategory(Integer userId, Integer categoryId) {
        if (userId < 0) {
            return 0;
        }
        String key = getRecentVisitCategoryKey(userId);
        return redisTemplate.execute(pushRecentVisitCategoryScript, Collections.singletonList(key), String.valueOf(categoryId), String.valueOf(System.currentTimeMillis()), String.valueOf(userRecentVisitCategoryNum));
    }

    public Result<Object> pinUserRecentVisitCategory(Integer categoryId, boolean isCancel, Operator operator) {
        String key = getRecentVisitCategoryKey(operator.getUserId());
        redisTemplate.execute(pinRecentVisitCategoryScript, Collections.singletonList(key), String.valueOf(categoryId), isCancel ? String.valueOf(System.currentTimeMillis()) : "", isCancel ? "0" : "1");
        return new Result<Object>(Result.SUCCESS);
    }

    private long pushUserRecentVisitCategory(Integer userId, String recordStr) {
        if (recordStr == null || recordStr.length() < 2) {
            return 0;
        }
        String key = getRecentVisitCategoryKey(userId);
        if (redisTemplate.hasKey(key)) {
            return 0;
        }
        String[] str = recordStr.split(";");
        Set<TypedTuple<String>> tupleSet = new HashSet<>(str.length);
        for (int i = 0; i < str.length; ++i) {
            String[] v = str[i].split("-");
            tupleSet.add(new DefaultTypedTuple<>(v[0], Double.valueOf(v[1])));
        }
        return redisTemplate.opsForZSet().add(key, tupleSet);
    }

    public Result<List<RecentVisitedCategory>> readRecentVisitedCategories(Operator operator) {
        String key = getRecentVisitCategoryKey(operator.getUserId());
        Set<TypedTuple<String>> resSet = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
        List<RecentVisitedCategory> categoryList = new ArrayList<>(resSet.size());
        for (TypedTuple<String> tuple : resSet) {
            Category category = categoryService.readCategory(Integer.valueOf(tuple.getValue()), false, operator).getObject();
            categoryList.add(new RecentVisitedCategory(category, tuple.getScore()));
        }
        return new Result<List<RecentVisitedCategory>>(SUCCESS).setObject(categoryList);
    }

    /**
     * 根据权限表重定义用户状态
     */
    public Result<Object> redefineUserState(Integer userId, Operator operator) {
        if (!userValidator.checkRedefineUserStatePermission(operator)) {
            return new Result<Object>(FAIL).setErrorCode(1010020221).setMessage("操作失败，权限错误");
        }
        if (dao.redefineState(userId) == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010020123).setMessage("数据库操作失败");
        }
        return new Result<Object>(Result.SUCCESS);
    }

    public void updateFollowingCnt(Integer userId, Long delta) {
        redisHelper.updateCounter(userId, "fln", delta);
    }

    public void updateFollowersCnt(Integer userId, Long delta) {
        redisHelper.updateCounter(userId, "flr", delta);
    }

    public void updatePostCnt(Integer userId, Long delta) {
        redisHelper.updateCounter(userId, "pst", delta);
    }

    private void setCounter(@NotNull User user) {
        Long[] cnt = redisHelper.getRedisCounterCnt(user.getId(), "fln", "flr", "pst");
        if (cnt[0] != null) {
            user.setFollowingCnt(user.getFollowingCnt() + cnt[0]);
        }
        if (cnt[1] != null) {
            user.setFollowersCnt(user.getFollowersCnt() + cnt[1]);
        }
        if (cnt[2] != null) {
            user.setPostCnt(user.getPostCnt() + cnt[2]);
        }
    }

    @SneakyThrows
    public <T> Result<Object> multiSetUser(@NotNull List<T> entityList, Method getUserIdMethod, Method setUserMethod) {
        Set<Integer> creatorIdSet = new HashSet<>(entityList.size());
        for (int i = 0; i < entityList.size(); ++i) {
            if (entityList.get(i) != null) {
                creatorIdSet.add((Integer) getUserIdMethod.invoke(entityList.get(i)));
            }
        }
        Result<List<User>> creatorListResult = multiReadUser(new ArrayList<>(creatorIdSet));
        if (creatorListResult.getState() == FAIL) {
            return new Result<Object>(creatorListResult);
        }
        List<User> creatorList = creatorListResult.getObject();
        Map<Integer, User> creatorMap = new HashMap<>(creatorList.size());
        for (int i = 0; i < creatorList.size(); ++i) {
            User creator = creatorList.get(i);
            if (creator != null) {
                creatorMap.put(creator.getId(), creator);
            }
        }
        for (int i = 0; i < entityList.size(); ++i) {
            if (entityList.get(i) != null) {
                User creator = creatorMap.get(getUserIdMethod.invoke(entityList.get(i)));
                if (creator != null) {
                    setUserMethod.invoke(entityList.get(i), creator);
                }
            }
        }
        return new Result<Object>(Result.SUCCESS);
    }

    /**
     * 创建用户
     */
    public Result<Integer> createUser(@NotNull User user) {
        if (!user.isValidInsertParam(true)) {
            return new Result<Integer>(FAIL).setErrorCode(1010020203).setMessage("操作错误，参数不合法");
        }
        Result<Object> checkNameResult = checkNickName(user.getNickName());
        if (checkNameResult.getState() == FAIL) {
            return new Result<Integer>(checkNameResult);
        }
        user.setPwdSalt(CryptUtil.randUrlSafeStr(32, true));
        user.setPassword(CryptUtil.md5(user.getPassword() + user.getPwdSalt()));
        int res = insertOne(user);
        if (res == -1) {
            return new Result<Integer>(FAIL).setErrorCode(1010020104).setMessage("数据库操作失败");
        }
        addNickNameIdx(user.getId(), user.getNickName());
        return new Result<Integer>(SUCCESS).setObject(user.getId());
    }

    public Result<User> readUserWithToken(Integer id) {
        User queryEntity = new User(id).setWithTokenInfo(true).setFromLogin(true);
        User user = selectOne(queryEntity);
        if (user == null || user.getToken() == null) {
            return new Result<User>(FAIL).setErrorCode(1010020205).setMessage("数据库操作失败");
        }
        redisHelper.setEntity(user, false);
//        setCounter(user);
        return new Result<User>(SUCCESS).setObject(user);
    }

    public Result<User> readUserWithQqOpenId(String openId) {
        User queryEntity = new User().setQqOpenId(openId).setWithTokenInfo(true).setFromLogin(true);
        User user = selectOne(queryEntity);
        if (user != null) {
            redisHelper.setEntity(user, false);
        }
        return new Result<User>(SUCCESS).setObject(user);
    }

    public Result<User> readUserWithCounter(Integer id, Operator operator) {
        return readUser(id, false, true, false, operator);
    }

    public Result<User> readUserWithAllFalse(Integer id, Operator operator) {
        return readUser(id, false, false, false, operator);
    }

    /**
     * 通过用户名获取用户
     */
    public Result<User> readUserByName(String name, boolean isFull, boolean withCnt, boolean withIsFollowedByViewer, Operator operator) {
        Integer userId = getUserIdByName(name);
        if (userId == null) {
            return new Result<User>(FAIL).setErrorCode(1010020224).setMessage("该用户名不存在");
        }
        return readUser(userId, isFull, withCnt, withIsFollowedByViewer, operator);
    }

    /**
     * 通过用户名模糊搜索用户
     */
    public Result<List<User>> readUserByNamePattern(String pattern, Operator operator) {
        List<Map.Entry<Object, Object>> resultList = getUserIdByNamePattern(pattern);
        if (resultList == null) {
            return new Result<List<User>>(FAIL).setErrorCode(1010020224).setMessage("用户模糊查询失败");
        }
        List<Integer> idList = new ArrayList<>();
        for (int i = 0; i < resultList.size(); ++i) {
            idList.add(Integer.valueOf((String) resultList.get(i).getValue()));
        }
        Result<List<User>> usrList = multiReadUser(idList);
        return usrList;
    }

    /**
     * 读取用户基本信息
     */
    public Result<User> readUser(Integer id, boolean isFull, boolean withCnt, boolean withIsFollowedByViewer, Operator operator) {
        User queryEntity = new User(id);
        Result<User> result;
        if (isFull) {
            // 读取用户完整信息直接读数据库
            queryEntity.setQueryParam(new QueryParam().setQueryType(QueryParam.FULL));
            result = queryHelper.readDbEntity(queryEntity);
        } else {
            result = queryHelper.readEntity(queryEntity, false);
        }
        if (result.getState() == FAIL) {
            return result;
        }
        User user = result.getObject();
        if (withCnt) {
            setCounter(user);
        }
        if (withIsFollowedByViewer) {
            List<Follow> followingList = followService.readFollowingList(operator.getUserId(), false, operator).getObject();
            if (followingList != null) {
                boolean flag = false;
                for (int i = 0; i < followingList.size(); ++i) {
                    if (id.equals(followingList.get(i).getTargetId())) {
                        flag = true;
                        break;
                    }
                }
                user.setFollowedByViewer(flag);
            }
        }
        return result;
    }

    public Result<UserRecvSetting> readUserRecvSetting(Integer id) {
        return recvSettingQueryHelper.readEntity(new UserRecvSetting(id), false);
    }

    public boolean existUser(Integer id) {
        return queryHelper.existEntity(new User(id));
    }

    public Result<List<User>> multiReadUser(List<Integer> idList) {
        return queryHelper.multiReadEntity(idList, false, new User());
    }

    public Result<User> login(@NotNull User paramUser) {
        if (paramUser.getNickName() == null || paramUser.getPassword() == null) {
            return new Result<User>(FAIL).setErrorCode(1010130206).setMessage("参数错误");
        }
        Integer userId = getUserIdByName(paramUser.getNickName());
        if (userId == null) {
            return new Result<User>(FAIL).setErrorCode(1010130207).setMessage("用户名不存在");
        }
        paramUser.setWithPwdInfo(true).setFromLogin(true).setId(userId);
        User resUser = selectOne(paramUser);
        if (resUser == null || resUser.getPassword() == null || resUser.getPwdSalt() == null) {
            return new Result<User>(FAIL).setErrorCode(1010020208).setMessage("未设置密码，请使用其他方式登录");
        }
        String testPwd = CryptUtil.md5(paramUser.getPassword() + resUser.getPwdSalt());
        if (!testPwd.equals(resUser.getPassword())) {
            return new Result<User>(FAIL).setErrorCode(1010130209).setMessage("密码错误");
        }
        return new Result<User>(SUCCESS).setObject(resUser);
    }

    /**
     * 处理登录后的用户数据加载
     */
    public Result<Object> afterLogin(User user, String token, boolean fromRegister) {
        // 更新数据库，增加登录次数
        if (dao.updateAfterLogin(user.getId(), token, new Date()) == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010020102).setMessage("数据库操作失败");
        }
        redisHelper.setEntity(user, false);
        if (!fromRegister) {
            pushUserRecentVisitCategory(user.getId(), user.getRecentVisitCategories());
            redisTemplate.opsForHash().putIfAbsent(getLastRefreshTimeKey(), String.valueOf(user.getId()), String.valueOf(user.getLastGetNewsTime().getTime()));
            setCounter(user);
        }
        return new Result<Object>(Result.SUCCESS);
    }

    /**
     * 更新用户信息
     */
    public Result<Object> updateUserInfo(@NotNull User paramUser, Operator operator) {
        if (!paramUser.isValidUpdateInfoParam(true)) {
            return new Result<Object>(FAIL).setErrorCode(1010020210).setMessage("操作错误，参数不合法");
        }
        // 检查操作者权限
        if (!userValidator.checkUpdatePermission(paramUser, operator)) {
            return new Result<Object>(FAIL).setErrorCode(1010020211).setMessage("操作失败，权限错误");
        }
        int res = updateOne(new User(paramUser.getId()).copyUpdateParamInfo(paramUser));
        if (res == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010020112).setMessage("数据库操作失败");
        }
        // 如果更新的是操作者自己，则更新本次请求中操作者的用户信息
        if (paramUser.getId().equals(operator.getUserId())) {
            operator.getUser().copyUpdateParamInfo(paramUser);
        }
        redisHelper.removeEntity(paramUser.getId());
        return new Result<Object>(Result.SUCCESS);
    }

    public Result<Object> setTopPost(User param, boolean isCancel, Operator operator) {
        if (!isCancel && param.getTopPostId() == null) {
            return new Result<Object>(FAIL).setErrorCode(1010010213).setMessage("操作失败，参数不合法");
        }
        Result<Post> topPostResult = postService.readPostWithAllFalse(param.getTopPostId(), operator);
        if (topPostResult.getState() == FAIL) {
            return new Result<Object>(topPostResult);
        }
        Integer topPostId = null;
        if (!isCancel) {
            Post topPost = topPostResult.getObject();
            if (!topPost.getCreatorId().equals(operator.getUserId())) {
                return new Result<Object>(FAIL).setErrorCode(1010010214).setMessage("操作失败，权限错误");
            }
            topPostId = param.getTopPostId();
        }
        int res = updateTopPost(operator.getUserId(), topPostId);
        // 数据库操作失败
        if (res == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010010117).setMessage("数据库操作失败");
        }
        operator.getUser().setTopPostId(topPostId);
        redisHelper.removeEntity(operator.getUser().getId());
        return new Result<Object>(Result.SUCCESS);
    }

    public Result<Object> updateUserName(@NotNull User paramUser, @NotNull Operator operator) {
        if (!paramUser.isValidUpdateNickNameParam(true)) {
            return new Result<Object>(FAIL).setErrorCode(1010020215).setMessage("操作错误，参数不合法");
        }
        String newName = paramUser.getNickName();
        Result<Object> checkNameResult = checkNickName(newName);
        if (checkNameResult.getState() == FAIL) {
            return checkNameResult;
        }
        String oriName = operator.getUser().getNickName();
        int res = updateOne(new User(operator.getUserId()).setNickName(newName).setLastAlterNameTime(new Date()));
        if (res == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010020116).setMessage("数据库操作失败");
        }
        alterNickNameIdx(operator.getUserId(), oriName, newName);
        operator.getUser().setNickName(newName);
        redisHelper.removeEntity(paramUser.getId());
        return new Result<Object>(Result.SUCCESS);
    }

    @NeedLogin
    public Result<Object> updatePassword(@NotNull User paramUser, @NotNull Operator operator) {
        if (!paramUser.isValidUpdatePasswordParam()) {
            return new Result<Object>(FAIL).setErrorCode(1010020225).setMessage("操作错误，参数不合法");
        }
        paramUser.setWithPwdInfo(true).setFromLogin(true).setId(operator.getUserId());
        User resUser = selectOne(paramUser);
        if (resUser == null || resUser.getPassword() == null || resUser.getPwdSalt() == null) {
            return new Result<Object>(FAIL).setErrorCode(1010020126).setMessage("数据库操作失败");
        }
        paramUser.setPassword(CryptUtil.md5(paramUser.getPassword() + resUser.getPwdSalt()));
        int res = updatePassword(paramUser);
        if (res == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010020227).setMessage("数据库操作失败");
        }
        return new Result<Object>(Result.SUCCESS);
    }

    @NeedLogin
    public Result<Object> updateRecvSetting(@NotNull UserRecvSetting param, Operator operator) {
        if (!param.isValidUpdateParam()) {
            return new Result<Object>(FAIL).setErrorCode(1010020217).setMessage("操作错误，参数不合法");
        }
        if (!param.getId().equals(operator.getUserId())) {
            return new Result<Object>(FAIL).setErrorCode(1010010218).setMessage("操作失败，权限错误");
        }
        int res = updateRecvSetting(param);
        if (res == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010020119).setMessage("数据库操作失败");
        }
        // 删除缓存
        recvSettingRedisHelper.removeEntity(param.getId());
        return new Result<Object>(Result.SUCCESS);
    }

    public Result<Object> setTalkBanTo(@NotNull User param, @NotNull Operator operator) {
        if (param.getId() == null || param.getTalkBanTo() == null || !param.isValidOpComment()) {
            return new Result<Object>(FAIL).setErrorCode(1010020220).setMessage("操作错误，参数不合法");
        }
        if (!userValidator.checkSetTalkBanToPermission(operator)) {
            return new Result<Object>(FAIL).setErrorCode(1010010221).setMessage("操作失败，权限错误");
        }
        int res = updateOne(new User(param.getId()).setTalkBanTo(param.getTalkBanTo()));
        if (res == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010020122).setMessage("数据库操作失败");
        }
        redisHelper.removeEntity(param.getId());
        return new Result<Object>(Result.SUCCESS);
    }

    public String correctCounters() {
        int r1 = dao.correctFollowersCnt();
        if (r1 == 0) {
            return "用户数据校正错误，请重新校正";
        }
        int r2 = dao.correctFollowingCnt();
        if (r2 == 0) {
            return "用户数据校正错误，请重新校正";
        }
        int r3 = dao.correctPostCnt();
        if (r3 == 0) {
            return "用户数据校正错误，请重新校正";
        }
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getCounterKey("*")));
        return "粉丝数校正" + r1 + "条数据;关注数校正" + r2 + "条数据;用户发帖数校正" + r3 + "条数据";
    }

    @SuppressWarnings("unchecked")
    @Scheduled(cron = "0 40 3 * * ?")
    public void dailyFlushRedisToDb() {
        List<TmpEntry>[] cntLists = redisHelper.flushRedisCounter("fln", "flr", "pst");
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getBasicKey("*")));
        commonService.batchUpdate("lpt_user", "user_id", "user_following_cnt", CommonService.OPS_INCR_BY, cntLists[0]);
        commonService.batchUpdate("lpt_user", "user_id", "user_followers_cnt", CommonService.OPS_INCR_BY, cntLists[1]);
        commonService.batchUpdate("lpt_user", "user_id", "user_post_cnt", CommonService.OPS_INCR_BY, cntLists[2]);
        log.info("用户的关注数、粉丝数以及发帖数写入数据库");
        Set<String> onlineOpKeys = RedisUtil.scanAllKeys(redisTemplate, operatorService.getBasicKeyPrefix());
        Set<Integer> onlineUserSet = new HashSet<>(onlineOpKeys.size());
        // 获取在线用户ID集合
        for (String key : onlineOpKeys) {
            onlineUserSet.add(Integer.valueOf(key.split(":")[1]));
        }
        String recentVisitKeyPrefix = getRecentVisitCategoryKey("*");
        Set<String> recentVisitKeys = RedisUtil.scanAllKeys(redisTemplate, recentVisitKeyPrefix);
        List<TmpEntry> tmpList = new ArrayList<>(recentVisitKeys.size());
        for (String key : recentVisitKeys) {
            Integer userId = Integer.valueOf(key.split(":")[1]);
            List<Object> resList = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                connection.zRevRangeWithScores(key.getBytes(), 0, -1);
                // 用户不在线，则清除redis中的访问记录
                if (!onlineUserSet.contains(userId)) {
                    connection.del(key.getBytes());
                }
                return null;
            });
            Set<TypedTuple<String>> categoryStrSet = (Set<TypedTuple<String>>) resList.get(0);
            if (categoryStrSet != null && categoryStrSet.size() != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                boolean first = true;
                for (TypedTuple<String> tuple : categoryStrSet) {
                    if (first) {
                        stringBuilder.append(tuple.getValue()).append('-').append((long) (double) tuple.getScore());
                        first = false;
                    } else {
                        stringBuilder.append(';').append(tuple.getValue()).append('-').append((long) (double) tuple.getScore());
                    }
                }
                tmpList.add(new TmpEntry(userId, stringBuilder.toString()));
            }
        }
        commonService.batchUpdate("lpt_user", "user_id", "user_recent_visit_categories", CommonService.OPS_COPY, tmpList);
        log.info("用户最近访问的目录记录写入数据库");
        Map<Object, Object> resMap = redisTemplate.opsForHash().entries(getLastRefreshTimeKey());
        tmpList = new ArrayList<>(resMap.size());
        List<Object> deleteKeys = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : resMap.entrySet()) {
            Integer userId = Integer.valueOf((String) entry.getKey());
            Date time = new Date(Long.valueOf((String) entry.getValue()));
            tmpList.add(new TmpEntry(userId, time));
            if (!onlineUserSet.contains(userId)) {
                deleteKeys.add(entry.getKey());
            }
        }
        if (deleteKeys.size() != 0) {
            redisTemplate.opsForHash().delete(getLastRefreshTimeKey(), deleteKeys.toArray());
        }
        commonService.batchUpdate("lpt_user", "user_id", "user_last_get_news_time", CommonService.OPS_COPY, tmpList);
        log.info("用户动态刷新时间写入数据库");
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public class RecentVisitedCategory {
        Category category;
        Double score;

        RecentVisitedCategory(Category category, Double score) {
            this.category = category;
            this.score = score;
        }
    }

}
