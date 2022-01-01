package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.BaseService;
import com.laputa.laputa_sns.common.QueryParam;
import com.laputa.laputa_sns.common.RedisPrefix;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.dao.PostDao;
import com.laputa.laputa_sns.executor.IndexExecutor;
import com.laputa.laputa_sns.model.entity.Notice;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.model.entity.User;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.QueryTokenUtil;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.validator.PostValidator;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * 转发服务
 *
 * @author JQH
 * @since 下午 3:15 20/03/08
 */

@EnableScheduling
@Service
public class ForwardService extends BaseService<PostDao, Post> {

    private final PostService postService;
    private final UserService userService;
    private final PostNewsService postNewsService;
    private final NoticeService noticeService;
    private final PostValidator postValidator;
    private final StringRedisTemplate redisTemplate;
    private final IndexExecutor.CallBacks<Post> indexExecutorCallBacks;

    private String hmacKey;

    public ForwardService(PostService postService, UserService userService, PostNewsService postNewsService, NoticeService noticeService, PostValidator postValidator, StringRedisTemplate redisTemplate) {
        this.postService = postService;
        this.userService = userService;
        this.postNewsService = postNewsService;
        this.noticeService = noticeService;
        this.postValidator = postValidator;
        this.redisTemplate = redisTemplate;
        this.indexExecutorCallBacks = initIndexExecutorCallBacks();
        this.hmacKey = CryptUtil.randUrlSafeStr(64, true);
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @NotNull
    private String getForwardZSetKey(Integer supId) {
        return RedisPrefix.FORWARD_RECORD_SET + ":" + supId;
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @NotNull
    private String getForwardZSetKeyPrefix() {
        return RedisPrefix.FORWARD_RECORD_SET + ":*";
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    private void addToForwardZSet(Integer supId, Integer forwardId) {
        String key = getForwardZSetKey(supId);
        redisTemplate.opsForZSet().add(key, String.valueOf(forwardId), System.currentTimeMillis());
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @NotNull
    private void addToForwardZSet(Integer supId, @NotNull List<Post> forwardList, IndexExecutor<Post>.Param param) {
        Set<Tuple> indexSet = new HashSet<>();
        for (int i = 0; i < forwardList.size(); ++i) {
            Post forward = forwardList.get(i);
            indexSet.add(new DefaultTuple(String.valueOf(forward.getId()).getBytes(), (double) (forward.getCreateTime().getTime())));
        }
        Object[] res = RedisUtil.addToZSetAndGetLastAndLength(redisTemplate, getForwardZSetKey(supId), indexSet, Integer.MAX_VALUE, false);
        if (res == null) {
            return;
        }
        param.newStartId = Integer.valueOf((String) res[0]);
        param.newFrom = (int) (long) res[1];
    }

    @Nullable
    private List<String> getRedisForwardIndex(@NotNull Post post) {
        String key = getForwardZSetKey(post.getSupId());
        List<String> resList = RedisUtil.zRevRangeUseStartIdFirst(redisTemplate, key, post.getQueryParam(), false);
        if (resList == null || (resList.size() == 1 && resList.get(0) == null)) {
            return null;
        }
        return resList;
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public void removeFromForwardZSet(Integer supId, Integer forwardId) {
        String key = getForwardZSetKey(supId);
        redisTemplate.opsForZSet().remove(key, String.valueOf(forwardId));
    }

    @Nullable
    private Result<List<Post>> getDbForwardList(@NotNull Post post, Operator operator) {
        return postService.readDbPostList(post, PostIndexService.LATEST, false, false, false, operator);
    }

    @NotNull
    private IndexExecutor.CallBacks<Post> initIndexExecutorCallBacks() {
        IndexExecutor.CallBacks<Post> callBacks = new IndexExecutor.CallBacks<>();
        callBacks.getIdListCallBack = executor -> {
            List<String> indexList = getRedisForwardIndex(executor.param.paramEntity);
            if (indexList == null) {
                executor.param.idList = null;
            } else {
                executor.param.idList = new ArrayList<>(indexList.size());
                for (int i = 0; i < indexList.size(); ++i) {
                    executor.param.idList.add(Integer.valueOf(indexList.get(i)));
                }
            }
        };
        callBacks.multiReadEntityCallBack = executor -> postService.multiReadPost(executor.param.paramEntity, executor.param.idList, true, false, false, false, false, false, executor.param.operator);
        callBacks.getDbListCallBack = executor -> getDbForwardList(executor.param.paramEntity, executor.param.operator);
        callBacks.multiSetRedisIndexCallBack = (entityList, executor) -> addToForwardZSet(executor.param.paramEntity.getSupId(), entityList, executor.param);
        return callBacks;
    }

    @SneakyThrows
    public Result<List<Post>> readForwardList(@NotNull Post post, Operator operator) {
        post.setOfType(Post.OF_SUP_POST);
        post.setType(Post.TYPE_FORWARD);
        if (!post.isValidReadForwardListParam()) {
            return new Result<List<Post>>(FAIL).setErrorCode(1010100204).setMessage("操作错误，参数不合法");
        }
        Result<Object> validateTokenResult = QueryTokenUtil.validateTokenAndSetQueryParam(post, PostIndexService.SUP_POST, hmacKey);
        if (validateTokenResult.getState() == FAIL) {
            return new Result<List<Post>>(validateTokenResult);
        }
        Result<Post> supPostResult = postService.readPostWithCounter(post.getSupId(), operator);
        if (supPostResult.getState() == FAIL) {
            return new Result<List<Post>>(supPostResult);
        }
        Post queryEntity = (Post) new Post().setQueryParam(new QueryParam());
        IndexExecutor<Post> indexExecutor = new IndexExecutor<Post>(post, queryEntity, null, indexExecutorCallBacks, operator);
        indexExecutor.param.childNumOfParent = supPostResult.getObject().getForwardCnt();
        Result<List<Post>> forwardListResult = indexExecutor.doIndex();
        if (forwardListResult.getState() == FAIL) {
            return forwardListResult;
        }
        List<Post> forwardList = forwardListResult.getObject();
        userService.multiSetUser(forwardList, Post.class.getMethod("getCreatorId"), Post.class.getMethod("setCreator", User.class));
        String newToken = QueryTokenUtil.generateQueryToken(post, forwardList, queryEntity.getQueryParam(), PostIndexService.SUP_POST, hmacKey);
        return new Result<List<Post>>(SUCCESS).setObject(forwardList).setAttachedToken(newToken);
    }

    /**
     * 创建转发，删除转发见PostService.deletePost
     */
    public Result<Integer> createForward(@NotNull Post post, Operator operator) {
        if (!post.isValidInsertForwardParam()) {
            return new Result<Integer>(FAIL).setErrorCode(1010100201).setMessage("操作错误，参数不合法");
        }
        Result<Post> supPostResult = postService.readPostWithAllFalse(post.getSupId(), operator);
        if (supPostResult.getState() == FAIL) {
            return new Result<Integer>(supPostResult);
        }
        Post supPost = supPostResult.getObject();
        if (!postValidator.checkCreateForwardPermission(supPost, operator)) {
            return new Result<Integer>(FAIL).setErrorCode(1010100202).setMessage("操作失败，权限错误");
        }
        post.setOriId(supPost.getOriId() == null ? supPost.getId() : supPost.getOriId())
                .setCategory(supPost.getCategory()).setLength(post.getContent().length())
                .setCreator(operator.getUser()).setType(Post.TYPE_FORWARD);
        int res = insertOne(post);
        if (res == -1) {
            return new Result<Integer>(FAIL).setErrorCode(1010050103).setMessage("数据库操作失败");
        }
        addToForwardZSet(post.getSupId(), post.getId());
        postService.updateForwardCnt(post.getSupId(), 1L);
        userService.updatePostCnt(post.getCreatorId(), 1L);
        postNewsService.pushNews(post.getCreatorId(), post.getId());
        if (!supPost.getCreatorId().equals(operator.getUserId())) {
            noticeService.pushNotice(supPost.getId(), Notice.TYPE_FW_POST, supPost.getCreatorId());
        }
        return new Result<Integer>(SUCCESS).setObject(post.getId());
    }

    /**
     * 每天将Redis中的转发记录索引删除
     */
    @Scheduled(cron = "0 00 3 * * ?")
    public void dailyFlushRedisForwardRecordToDb() {
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, getForwardZSetKeyPrefix()));
        // 更新hmac的key
        hmacKey = CryptUtil.randUrlSafeStr(64, true);
    }

}
