package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.dao.PostNewsDao;
import com.laputa.laputa_sns.executor.IndexExecutor;
import com.laputa.laputa_sns.model.entity.Follow;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.model.entity.PostNews;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.QueryTokenUtil;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.util.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * 内容动态服务(Feed流)
 * 等有钱了，Feed流改成推模式，现在暂时用拉模式
 *
 * @author JQH
 * @since 下午 5:43 20/03/09
 */

@Service
@EnableScheduling
@Slf4j
@Order(3)
public class PostNewsService extends BaseService<PostNewsDao, PostNews> implements ApplicationRunner {

    private final FollowService followService;
    private final PostService postService;
    private final StringRedisTemplate redisTemplate;
    private final IndexExecutor.CallBacks indexExecutorCallBacks;
    private final DefaultRedisScript<Long> pushNewsScript =
            new DefaultRedisScript(ResourceUtil.getString("/lua/news/pushNews.lua"), Long.class);//redis 5.0之后可以使用ZPOPMIN命令;
    private final DefaultRedisScript<List> pollNewsScript =
            new DefaultRedisScript(ResourceUtil.getString("/lua/news/pollNews.lua"), List.class);
    private final DefaultRedisScript<Long> pollNewsCountScript =
            new DefaultRedisScript(ResourceUtil.getString("/lua/news/pollNewsCount.lua"), Long.class);//必须用Long类型来接收Redis返回的整型数
    private final DefaultRedisScript<Long> multiPushNewsScript =
            new DefaultRedisScript(ResourceUtil.getString("/lua/news/multiPushNews.lua"), Long.class);

    private String hmacKey;

    public PostNewsService(FollowService followService, PostService postService, StringRedisTemplate redisTemplate) {
        this.followService = followService;
        this.postService = postService;
        this.redisTemplate = redisTemplate;
        this.hmacKey = CryptUtil.randUrlSafeStr(64, true);
        this.indexExecutorCallBacks = initIndexExecutorCallBacks();
    }

    @Value("${user-news-out-box-length}")//#用户动态发件箱最大长度
    private int userNewsOutBoxLength;

    @Value("${user-news-in-box-length}")//#用户动态收件箱最大长度
    private int userNewsInBoxLength;

    @Override
    /**加载全部用户的发件箱*/
    public void run(ApplicationArguments args) {
        if (redisTemplate.hasKey(RedisPrefix.USER_NEWS_OUT_BOX_INIT)) {
            log.info("用户发件箱已存在");
            Globals.UserNewsBoxInitialized = true;
            return;
        }
        redisTemplate.opsForValue().set(RedisPrefix.USER_NEWS_OUT_BOX_INIT, "1");
        int queryNum = 5000;
        int startId = Integer.MAX_VALUE;//倒着加载
        int listLimit = userNewsOutBoxLength * 2;
        long addCnt = 0;
        while (true) {
            List<Post> postList = dao.selectPostListOfAllUser(startId, queryNum);
            HashMap<String, List<String>> userPostMap = new HashMap();
            if (postList == null || postList.size() == 0)
                break;
            for (int i = 0; i < postList.size(); ++i) {
                String key = getNewsOutBoxKey(postList.get(i).getCreatorId());
                List<String> valueStrList = userPostMap.get(key);
                if (valueStrList == null) {
                    valueStrList = new ArrayList();
                    userPostMap.put(key, valueStrList);
                } else if (valueStrList.size() >= listLimit)//倒着加载，先读出来的肯定是新的
                    continue;
                valueStrList.add(String.valueOf(postList.get(i).getCreateTime().getTime()));
                valueStrList.add(String.valueOf(postList.get(i).getId()));
            }
            startId = postList.get(postList.size() - 1).getId();
            List<String> keys = new ArrayList(userPostMap.size());
            List<String> argv = new ArrayList();
            argv.add(String.valueOf(userNewsOutBoxLength));
            for (Map.Entry<String, List<String>> entry : userPostMap.entrySet()) {
                keys.add(entry.getKey());
                keys.add(String.valueOf(entry.getValue().size()));
                argv.addAll(entry.getValue());
            }
            addCnt += redisTemplate.execute(multiPushNewsScript, keys, argv.toArray());
            //TimeUnit.SECONDS.sleep(1);
        }
        Globals.UserNewsBoxInitialized = true;
        log.info("所有用户发件箱加载完成，共" + addCnt + "条数据");
    }

    private List<PostNews> selectList(PostNews entity, List<Follow> followingList) {
        return dao.selectList(entity, followingList);
    }

    private String getNewsOutBoxKey(Integer userId) {
        return RedisPrefix.USER_NEWS_OUT_BOX + ":" + userId;
    }

    private String getNewsInBoxKey(Integer userId) {
        return RedisPrefix.USER_NEWS_IN_BOX + ":" + userId;
    }

    private String getNewsInBoxKey(String str) {
        return RedisPrefix.USER_NEWS_IN_BOX + ":" + str;
    }

    private String getLastRefreshTimeKey() {
        return RedisPrefix.USER_LAST_REFRESH_TIME;
    }

    public long pushNews(Integer userId, Integer postId) {
        return redisTemplate.execute(pushNewsScript, Collections.singletonList(getNewsOutBoxKey(userId)), String.valueOf(userNewsOutBoxLength), String.valueOf(new Date().getTime()), String.valueOf(postId));
    }

    @NotNull
    private Object[] multiSetRedisInBoxIndexAndGetLast(Integer userId, @NotNull List<PostNews> newsList, boolean replaceWhenFull) {
        Set<RedisZSetCommands.Tuple> indexSet = new HashSet();
        for (int i = 0; i < newsList.size(); ++i) {
            PostNews news = newsList.get(i);
            if (news == null || news.getId() == null)//如果一条动态重复插入会导致插入失败而使news对象为空
                continue;
            Integer newsId = i == newsList.size() - 1 ? news.getId() : null;
            indexSet.add(new DefaultTuple(getRedisValue(news.getPostId(), newsId).getBytes(), getInBoxScore(news)));
        }
        return RedisUtil.addToZSetAndGetLastAndLength(redisTemplate, getNewsInBoxKey(userId), indexSet, userNewsInBoxLength, replaceWhenFull);
    }

    private class RedisValue {
        Integer postId;
        Integer newsId;

        RedisValue(Integer postId, Integer newsId) {
            this.postId = postId;
            this.newsId = newsId;
        }
    }

    private String getRedisValue(Integer postId, Integer newsId) {
        if (newsId == null)
            return String.valueOf(postId);
        return postId + ":" + newsId;
    }

    @NotNull
    private RedisValue parseRedisValue(@NotNull String v) {
        String[] s = v.split(":");
        if (s.length == 1)
            return new RedisValue(Integer.valueOf(s[0]), null);
        return new RedisValue(Integer.valueOf(s[0]), Integer.valueOf(s[1]));
    }

    private double getInBoxScore(@NotNull PostNews news) {
        return news.getId();
    }

    public Result<Integer> readNewsCount(@NotNull Operator operator) {
        Integer followerId = operator.getUserId();
        Result<List<Follow>> targetListResult = followService.readFollowingList(followerId, false, operator);
        if (targetListResult.getState() == FAIL)
            return (Result) targetListResult;
        List<Follow> targetList = targetListResult.getObject();
        operator.setFollowList(targetList);
        List<String> keys = new ArrayList(targetList.size() + 1);
        keys.add(getLastRefreshTimeKey());
        for (int i = 0; i < targetList.size(); ++i)
            keys.add(getNewsOutBoxKey(targetList.get(i).getTargetId()));
        Integer res = (int) (long) redisTemplate.execute(pollNewsCountScript, keys, String.valueOf(followerId));
        return new Result(SUCCESS).setObject(res);
    }

    @NotNull
    private List<String> pollNews(Integer followerId, @NotNull List<Follow> targetList) {
        List<String> keys = new ArrayList(targetList.size() + 1);
        keys.add(getLastRefreshTimeKey());
        for (int i = 0; i < targetList.size(); ++i)
            keys.add(getNewsOutBoxKey(targetList.get(i).getTargetId()));
        return redisTemplate.execute(pollNewsScript, keys, String.valueOf(followerId));
    }

    public void deleteNews(Integer userId, Integer postId) {
        redisTemplate.opsForZSet().remove(getNewsOutBoxKey(userId), String.valueOf(postId));
    }

    /**
     * 根据关注列表返回已经取关但还在列表中的PostNews对应的Redis中ZSet的Value
     * @param postNewsList
     * @param numLimit
     * @param followingList
     * @param operator
     * @return
     */
    private Result<List<String>> fillPostNewsWithPost(List<PostNews> postNewsList, Integer numLimit, List<Follow> followingList, Operator operator) {
        int limit = numLimit == null ? postNewsList.size() : Math.min(numLimit, postNewsList.size());
        List<Integer> postIdList = new ArrayList(limit);
        for (int i = 0; i < limit; ++i)
            if (postNewsList.get(i).getContent() == null)//已经被填上了，不用再填
                postIdList.add(postNewsList.get(i).getPostId());
        Result<List<Post>> postListResult = postService.multiReadPost(null, postIdList, true, true, true, true, true, true, operator);
        if (postListResult.getState() == FAIL)
            return (Result) postListResult;
        List<Post> postList = postListResult.getObject();
        postService.multiSetOriPost(postList, operator);
        HashSet<Integer> followingSet = null;
        if (followingList != null) {//传入的关注列表，用于去掉取关的但还在Redis缓存中的帖子
            followingSet = new HashSet(followingList.size());
            for (int i = 0; i < followingList.size(); ++i)
                followingSet.add(followingList.get(i).getTargetId());
        }
        HashMap<Integer, Post> postResultMap = new HashMap(postList.size());
        for (int i = 0; i < postList.size(); ++i)
            if (postList.get(i) != null)
                postResultMap.put(postList.get(i).getId(), postList.get(i));
        List<String> remValues = new ArrayList();
        for (int i = 0; i < postNewsList.size(); ++i) {
            PostNews news = postNewsList.get(i);
            Post post = postResultMap.get(news.getPostId());
            if (post != null) {
                if (followingSet != null && !followingSet.contains(post.getCreatorId())) {
                    remValues.add(getRedisValue(post.getId(), news.getId()));//news.getId()可能为空
                } else
                    news.setContent(post);
            }
        }
        return new Result(SUCCESS).setObject(remValues);
    }

    /**
     * 目前没有FAIL
     */
    public Result<List<Integer>> readNewsPostIdListOfCreator(Integer creatorId, QueryParam queryParam) {
        List<String> resList = RedisUtil.zRevRangeUseStartIdFirst(redisTemplate, getNewsOutBoxKey(creatorId), queryParam, false);
        if (resList == null || (resList.size() == 1 && resList.get(0) == null))
            return new Result(SUCCESS).setObject(new ArrayList(0));
        List<Integer> postIdList = new ArrayList(resList.size());
        for (int i = 0; i < resList.size(); ++i)
            postIdList.add(Integer.valueOf(resList.get(i)));
        return new Result(SUCCESS).setObject(postIdList);
    }

    @NotNull
    private IndexExecutor.CallBacks initIndexExecutorCallBacks() {
        IndexExecutor.CallBacks callBacks = new IndexExecutor.CallBacks();
        callBacks.getIdListCallBack = executor -> {
            IndexExecutor.Param param = executor.param;
            Integer userId = param.operator.getUserId();
            Integer unreadCnt = param.operator.getUnreadNewsCnt();
            List<Follow> followList = (List) param.addition;
            if (unreadCnt != null && !unreadCnt.equals(0) && followList != null && followList.size() != 0) {
                List<String> newsValueList = pollNews(userId, followList);
                List<PostNews> dbList = new ArrayList(newsValueList.size());
                for (int i = 0; i < newsValueList.size(); ++i) {
                    String[] v = newsValueList.get(i).split("-");
                    dbList.add(new PostNews().setReceiverId(userId).setPostId(Integer.valueOf(v[0]))
                            .setSenderId(Integer.valueOf(v[1].split(":")[1])));
                }
                dbList.sort(Comparator.comparingInt(PostNews::getPostId));//放数据库时ID从小到大
                int res = insertList(dbList);//将未读的放入数据库
                if (res != 0)//插入数据库后即为已读
                    redisTemplate.opsForHash().put(getLastRefreshTimeKey(), String.valueOf(userId), String.valueOf(new Date().getTime()));
                multiSetRedisInBoxIndexAndGetLast(userId, dbList, true);
                Collections.reverse(dbList);
                int queryNum = param.paramEntity.getQueryParam().getQueryNum();
                if (dbList.size() >= queryNum) {
                    param.idList = new ArrayList(queryNum);
                    for (int i = 0; i < queryNum; ++i)
                        param.idList.add(dbList.get(i).getPostId());
                    Integer newsId = dbList.size() == queryNum ? dbList.get(queryNum - 1).getId() : null;//请求的个数正好等于获取的个数，那么value中包含newsId
                    executor.param.newStartValue = getRedisValue(dbList.get(queryNum - 1).getPostId(), newsId);
                    executor.param.newStartId = dbList.get(queryNum - 1).getId();
                    return;
                }
            }
            List<TypedTuple<String>> indexList = RedisUtil.readIndex(redisTemplate, getNewsInBoxKey(userId), param.paramEntity.getQueryParam(), true, false);
            List<Integer> idList = null;
            if (indexList != null) {
                idList = new ArrayList(indexList.size());
                for (int i = 0; i < indexList.size(); ++i)
                    idList.add(parseRedisValue(indexList.get(i).getValue()).postId);
                if (indexList.size() != 0) {
                    param.newStartValue = indexList.get(indexList.size() - 1).getValue();
                    param.newStartId = parseRedisValue(param.newStartValue).newsId;
                }
            }
            param.indexSetList = indexList;
            param.idList = idList;
        };
        callBacks.multiReadEntityCallBack = executor -> {
            IndexExecutor.Param param = executor.param;
            List<PostNews> newsList = new ArrayList(param.idList.size());
            for (int i = 0; i < param.idList.size(); ++i) {
                Integer newsId = null;
                if (param.indexSetList != null)
                    newsId = parseRedisValue(((List<TypedTuple<String>>) param.indexSetList).get(i).getValue()).newsId;
                newsList.add(new PostNews(newsId).setReceiverId(param.operator.getUserId()).setPostId((Integer) param.idList.get(i)));
            }
            Result<List<String>> remValuesResult = fillPostNewsWithPost(newsList, param.paramEntity.getQueryParam().getQueryNum(), (List<Follow>) param.addition, param.operator);
            List<String> remValues = remValuesResult.getObject();
            if (remValuesResult.getState() == SUCCESS && remValues.size() != 0)
                redisTemplate.opsForZSet().remove(getNewsInBoxKey(param.operator.getUserId()), remValues.toArray());
            List<PostNews> tmpList = new ArrayList(newsList.size() - remValues.size());
            for (int i = 0; i < newsList.size(); ++i)
                if (newsList.get(i).getContent() != null)//剔除掉已经取关的内容
                    tmpList.add(newsList.get(i));
            return new Result(SUCCESS).setObject(tmpList);
        };
        callBacks.getDBListCallBack = executor -> {
            PostNews param = (PostNews) executor.param.paramEntity;
            if (param.getQueryParam().getStartId() == null || param.getQueryParam().getStartId().equals(0)) {
                RedisValue redisValue = parseRedisValue(param.getQueryParam().getStartValue());
                if (redisValue.newsId != null)
                    param.getQueryParam().setStartId(redisValue.newsId).setUseStartIdAtSql(1);
                else
                    param.setPostId(redisValue.postId).getQueryParam().setUseStartIdAtSql(0);
            } else
                param.getQueryParam().setUseStartIdAtSql(1);
            List<PostNews> resList = selectList(param, (List) executor.param.addition);
            if (resList == null)
                return new Result(FAIL).setErrorCode(1010120103).setMessage("数据库操作错误");
            fillPostNewsWithPost(resList, null, (List) executor.param.addition, executor.param.operator);
            return new Result(SUCCESS).setObject(resList);
        };
        callBacks.multiSetRedisIndexCallBack = (entityList, executor) -> {
            Object[] res = multiSetRedisInBoxIndexAndGetLast(executor.param.operator.getUserId(), entityList, false);
            if (res == null)
                return;
            executor.param.newStartValue = (String) res[0];
            executor.param.newFrom = (int) (long) res[1];
            executor.param.newStartId = ((PostNews) entityList.get(entityList.size() - 1)).getId();
        };
        return callBacks;
    }

    public Result<List<PostNews>> readNewsList(PostNews param, @NotNull Operator operator) {
        Integer userId = operator.getUserId();
        if (userId.equals(-1))
            return new Result(FAIL).setErrorCode(1010120204).setMessage("未登录");
        if (!param.isValidReadNewsParam())
            return new Result(FAIL).setErrorCode(1010120205).setMessage("操作错误，参数不合法");
        param.setReceiverId(userId);
        Result validateTokenResult = QueryTokenUtil.validateTokenAndSetQueryParam(param, 0, hmacKey);
        if (validateTokenResult.getState() == FAIL)
            return validateTokenResult;
        List<Follow> followingList;
        if (operator.getFollowList() != null)
            followingList = operator.getFollowList();
        else {
            Result<List<Follow>> followingListResult = followService.readFollowingList(userId, false, operator);
            if (followingListResult.getState() == FAIL)
                return (Result) followingListResult;
            followingList = followingListResult.getObject();
        }
        if (followingList.size() == 0)
            return new Result(SUCCESS).setObject(new ArrayList(0));
        PostNews queryEntity = (PostNews) new PostNews().setQueryParam(new QueryParam());
        IndexExecutor indexExecutor = new IndexExecutor(param, queryEntity, null, indexExecutorCallBacks, operator);
        indexExecutor.param.addition = followingList;
        indexExecutor.param.initStartId = Integer.MAX_VALUE;
        indexExecutor.param.childNumOfParent = Long.MAX_VALUE;
        Result<List<PostNews>> newsListResult = indexExecutor.doIndex();
        if (newsListResult.getState() == FAIL)
            return newsListResult;
        String newToken = QueryTokenUtil.generateQueryToken(param, newsListResult.getObject(), queryEntity.getQueryParam(), 0, hmacKey);
        return newsListResult.setAttachedToken(newToken);
    }

    @Scheduled(cron = "0 40 2 * * ?")
    public void dailyFreshIndex() {
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, getNewsInBoxKey("*")));
        log.info("用户动态收件箱缓存清除");
        hmacKey = CryptUtil.randUrlSafeStr(64, true);//更新hmac的key
    }
}
