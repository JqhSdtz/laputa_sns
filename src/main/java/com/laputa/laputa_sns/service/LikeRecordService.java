package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.dao.LikeRecordDao;
import com.laputa.laputa_sns.executor.IndexExecutor;
import com.laputa.laputa_sns.model.entity.*;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.QueryTokenUtil;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.validator.LikeRecordValidator;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * 点赞记录相关的服务
 *
 * @author JQH
 * @since 下午 2:11 20/02/24
 */

@Slf4j
@EnableScheduling
@Service
public class LikeRecordService extends BaseService<LikeRecordDao, LikeRecord> {

    private final PostService postService;
    private final CommentL1Service commentL1Service;
    private final CommentL2Service commentL2Service;
    private final UserService userService;
    private final NoticeService noticeService;
    private final CommonService commonService;
    private final IndexExecutor.CallBacks<LikeRecord> indexExecutorCallBacks;
    private final StringRedisTemplate redisTemplate;
    private final LikeRecordValidator likeRecordValidator;
    private final String[] typeStr = {"PS", "C1", "C2"};
    private String hmacKey;

    public LikeRecordService(PostService postService, @Lazy CommentL1Service commentL1Service, @Lazy CommentL2Service commentL2Service, UserService userService, NoticeService noticeService, CommonService commonService, @NotNull StringRedisTemplate redisTemplate, LikeRecordValidator likeRecordValidator) {
        this.postService = postService;
        this.commentL1Service = commentL1Service;
        this.commentL2Service = commentL2Service;
        this.userService = userService;
        this.noticeService = noticeService;
        this.commonService = commonService;
        this.redisTemplate = redisTemplate;
        this.likeRecordValidator = likeRecordValidator;
        this.hmacKey = CryptUtil.randUrlSafeStr(64, true);
        this.indexExecutorCallBacks = initIndexExecutorCallBacks();
    }

    /**
     * 获取redis中新点赞列表的key，新点赞列表是指没有被刷入数据库的，会被用作重复校验的点赞列表
     *
     * @param targetId
     * @param type
     * @return
     */
    @NotNull
    private String getNewRedisSetKey(int targetId, int type) {
        return RedisPrefix.LIKE_RECORD_SET_NEW + "/" + typeStr[type] + ":" + targetId;
    }

    @NotNull
    private String getNewRedisSetKeyPrefix(int type) {
        return RedisPrefix.LIKE_RECORD_SET_NEW + "/" + typeStr[type] + ":*";
    }

    /**
     * 获取redis中旧点赞列表的key，旧点赞列表是指已经被刷入数据库后又被取出来的，仅用于展示的
     * 点赞列表，一般不会用于重复校验
     *
     * @param targetId
     * @param type
     * @return
     */
    @NotNull
    private String getFormerRedisSetKey(int targetId, int type) {
        return RedisPrefix.LIKE_RECORD_SET_FORMER + "/" + typeStr[type] + ":" + targetId;
    }

    @NotNull
    private String getFormerRedisSetKeyPrefix(int type) {
        return RedisPrefix.LIKE_RECORD_SET_FORMER + "/" + typeStr[type] + ":*";
    }

    /**
     * 获取该对象的点赞列表的长度，即当日点赞数，从set中获取，list不一定准
     */
    public long getNewRedisLikeSetSize(int targetId, int type) {
        return redisTemplate.opsForZSet().size(getNewRedisSetKey(targetId, type));
    }

    /**
     * 从redis中获取多个被点赞对象的点赞数量
     *
     * @param targetIdList
     * @param type
     * @return
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private List<Long> multiGetRedisLikeSetSize(@NotNull List<Integer> targetIdList, int type) {
        List<String> keys = new ArrayList<>(targetIdList.size());
        for (int i = 0; i < targetIdList.size(); ++i) {
            keys.add(getNewRedisSetKey(targetIdList.get(i), type));
        }
        List<Object> resList = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            for (int i = 0; i < keys.size(); ++i) {
                connection.zCard(keys.get(i).getBytes());
            }
            return null;
        });
        return (List<Long>) (List<?>) resList;
    }

    /**
     * 给某个内容对象设置点赞数量
     *
     * @param entity
     * @param type
     */
    public void setLikeCnt(@NotNull AbstractContent<?> entity, int type) {
        entity.setLikeCnt(entity.getLikeCnt() + getNewRedisLikeSetSize(entity.getId(), type));
    }

    /**
     * 给多个内容对象设置点赞数量
     *
     * @param entityList
     * @param type
     * @param <T>
     */
    public <T extends AbstractContent<T>> void multiSetLikeCnt(@NotNull List<T> entityList, int type) {
        List<Integer> idList = new ArrayList<>(entityList.size());
        for (int i = 0; i < entityList.size(); ++i) {
            idList.add(entityList.get(i) == null ? -1 : entityList.get(i).getId());
        }
        List<Long> likeCntList = multiGetRedisLikeSetSize(idList, type);
        for (int i = 0; i < likeCntList.size(); ++i) {
            AbstractContent<T> entity = entityList.get(i);
            if (entity != null) {
                entity.setLikeCnt(entity.getLikeCnt() + likeCntList.get(i));
            }
        }
    }

    @NotNull
    private String getRedisValue(Integer creatorId, Integer likeRecordId) {
        if (likeRecordId == null) {
            return String.valueOf(creatorId);
        }
        return creatorId + ":" + likeRecordId;
    }

    @NotNull
    private RedisValue parseRedisValue(@NotNull String v) {
        String[] s = v.split(":");
        if (s.length == 1) {
            return new RedisValue(Integer.valueOf(s[0]), null);
        }
        return new RedisValue(Integer.valueOf(s[0]), Integer.valueOf(s[1]));
    }

    /**
     * 向Redis中添加点赞记录
     */
    private boolean addNewLikeRecord(@NotNull LikeRecord likeRecord) {
        String v = getRedisValue(likeRecord.getCreatorId(), null);
        return redisTemplate.opsForZSet().add(getNewRedisSetKey(likeRecord.getTargetId(), likeRecord.getType()), v, System.currentTimeMillis());
    }

    /**
     * 删除Redis中的点赞记录，仅删除set中记录，在list中删除复杂度高
     */
    private boolean removeLikeRecord(@NotNull LikeRecord likeRecord) {
        String v = getRedisValue(likeRecord.getCreatorId(), null);
        long res = redisTemplate.opsForZSet().remove(getNewRedisSetKey(likeRecord.getTargetId(), likeRecord.getType()), v);
        return res != 0;
    }

    /**
     * 判断Redis中是否有某条点赞记录
     */
    public boolean existLikeRecordInRedis(@NotNull LikeRecord likeRecord) {
        String key = getNewRedisSetKey(likeRecord.getTargetId(), likeRecord.getType());
        String v = getRedisValue(likeRecord.getCreatorId(), null);
        return redisTemplate.opsForZSet().score(key, v) != null;
    }

    /**
     * 给多个内容对象设置是否被某个用户点过赞
     * 这里仅判断是否在新的点赞列表中，即还未刷入数据库的记录
     * 已刷入数据库的记录不会用来判断是否点赞
     *
     * @param targetList
     * @param type
     * @param userId
     * @param <T>
     */
    public <T extends AbstractContent<T>> void multiSetIsLikedByViewer(@NotNull List<T> targetList, int type, Integer userId) {
        byte[] creatorKey = getRedisValue(userId, null).getBytes();
        List<Object> resList = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            for (int i = 0; i < targetList.size(); ++i) {
                connection.zScore(getNewRedisSetKey(targetList.get(i) == null ? -1 : targetList.get(i).getId(), type).getBytes(), creatorKey);
            }
            return null;
        });
        for (int i = 0; i < targetList.size(); ++i) {
            if (targetList.get(i) == null) {
                continue;
            }
            Object resObj = resList.get(i);
            if (resObj == null) {
                targetList.get(i).setLikedByViewer(false);
            } else if (resObj instanceof Boolean) {
                targetList.get(i).setLikedByViewer((Boolean) resObj);
            } else if (resObj instanceof Double) {
                targetList.get(i).setLikedByViewer(((Double) resObj) != 0);
            } else {
                log.warn("类型" + resObj.getClass().getName() + "无法判断点赞");
                targetList.get(i).setLikedByViewer(false);
            }
        }
    }

    /**
     * 获取点赞目标，帖子、一级评论或二级评论
     */
    private Result<? extends AbstractContent<?>> getTarget(@NotNull LikeRecord likeRecord, boolean withCounterOfComment, Operator operator) {
        AbstractContent<?> target = null;
        if (likeRecord.getType().equals(LikeRecord.TYPE_POST)) {
            Result<Post> postResult = postService.readPostWithCounter(likeRecord.getTargetId(), operator);
            // 没有该帖，返回错误
            if (postResult.getState() == FAIL) {
                return postResult;
            }
            target = postResult.getObject();
        } else if (likeRecord.getType().equals(LikeRecord.TYPE_CML1)) {
            Result<CommentL1> commentResult = withCounterOfComment ? commentL1Service.readCommentWithCounter(likeRecord.getTargetId(), operator) : commentL1Service.readCommentWithAllFalse(likeRecord.getTargetId(), operator);
            // 没有该帖，返回错误
            if (commentResult.getState() == FAIL) {
                return commentResult;
            }
            target = commentResult.getObject();
        } else if (likeRecord.getType().equals(LikeRecord.TYPE_CML2)) {
            Result<CommentL2> commentResult = withCounterOfComment ? commentL2Service.readCommentWithCounter(likeRecord.getTargetId(), operator) : commentL2Service.readCommentWithAllFalse(likeRecord.getTargetId(), operator);
            if (commentResult.getState() == FAIL) {
                return commentResult;
            }
            target = commentResult.getObject();
        }
        return new Result<AbstractContent<?>>(SUCCESS).setObject(target);
    }

    @NotNull
    private IndexExecutor.CallBacks<LikeRecord> initIndexExecutorCallBacks() {
        IndexExecutor.CallBacks<LikeRecord> callBacks = new IndexExecutor.CallBacks<>();
        callBacks.getIdListCallBack = executor -> {
            LikeRecord param = executor.param.paramEntity;
            int queryNum = param.getQueryParam().getQueryNum();
            // 是否从新的点赞列表中获取，新的点赞列表指还未刷入数据库的，一个用户在某个对象的新的点赞列表中不会重复出现
            boolean fromNewZset = param.getQueryParam().getAddition() == null || !"1".equals(param.getQueryParam().getAddition());
            executor.param.addition = fromNewZset;
            String key = fromNewZset ? getNewRedisSetKey(param.getTargetId(), param.getType()) : getFormerRedisSetKey(param.getTargetId(), param.getType());
            List<TypedTuple<String>> indexList = RedisUtil.readIndex(redisTemplate, key, param.getQueryParam(), true, true);
            // 当前是从新的点赞列表获取，但新的不够了，就再从redis中旧的点赞列表中获取
            // 但要注意这和从数据库中补充不是一回事
            if (fromNewZset && (indexList == null || indexList.size() < queryNum)) {
                executor.param.queryEntity.getQueryParam().setAddition("1");
                int dim = indexList == null ? 0 : indexList.size();
                param.getQueryParam().setQueryNum(queryNum - dim).setFrom(0).setStartId(0).setStartValue(null);
                String key2 = getFormerRedisSetKey(param.getTargetId(), param.getType());
                List<TypedTuple<String>> indexList2 = RedisUtil.readIndex(redisTemplate, key2, param.getQueryParam(), true, true);
                if (indexList2 != null && indexList2.size() != 0) {
                    if (indexList == null) {
                        indexList = indexList2;
                    } else {
                        indexList.addAll(indexList2);
                    }
                }
            }
            List<Integer> idList = null;
            if (indexList != null) {
                idList = new ArrayList<>(indexList.size());
                for (int i = 0; i < indexList.size(); ++i) {
                    idList.add(parseRedisValue(indexList.get(i).getValue()).creatorId);
                }
                if (indexList.size() != 0) {
                    executor.param.newStartValue = indexList.get(indexList.size() - 1).getValue();
                    executor.param.newStartId = parseRedisValue(executor.param.newStartValue).likeRecordId;
                }
            }
            executor.param.idList = idList;
            executor.param.indexSetList = indexList;
        };
        callBacks.multiReadEntityCallBack = executor -> {
            IndexExecutor<LikeRecord>.Param param = executor.param;
            Result<List<User>> creatorListResult = userService.multiReadUser(param.idList);
            if (creatorListResult.getState() == FAIL) {
                return new Result<List<LikeRecord>>(creatorListResult);
            }
            List<User> creatorList = creatorListResult.getObject();
            List<LikeRecord> likeRecordList = new ArrayList<>(param.idList.size());
            for (int i = 0; i < param.idList.size(); ++i) {
                TypedTuple<String> tuple = param.indexSetList.get(i);
                likeRecordList.add((LikeRecord) new LikeRecord().setCreator(creatorList.get(i)).setCreateTime(new Date(Math.round(tuple.getScore()))));
            }
            return new Result<List<LikeRecord>>(SUCCESS).setObject(likeRecordList);
        };
        callBacks.getDbListCallBack = executor -> {
            LikeRecord param = executor.param.paramEntity;
            boolean fromNewZset = (boolean) executor.param.addition;
            if (param.getQueryParam().getStartId() == null || param.getQueryParam().getStartId().equals(0)) {
                RedisValue redisValue = parseRedisValue(param.getQueryParam().getStartValue());
                if (redisValue.likeRecordId != null) {
                    param.getQueryParam().setStartId(redisValue.likeRecordId).setUseStartIdAtSql(1);
                } else {
                    if (fromNewZset) {
                        param.getQueryParam().setStartId(executor.param.initStartId).setUseStartIdAtSql(1);
                    } else {
                        param.setCreatorId(redisValue.creatorId).getQueryParam().setUseStartIdAtSql(0);
                    }
                }
            } else {
                param.getQueryParam().setUseStartIdAtSql(1);
            }
            List<LikeRecord> resList = selectList(param);
            if (resList == null) {
                return new Result<List<LikeRecord>>(FAIL).setErrorCode(1010070104).setMessage("数据库操作错误");
            }
            userService.multiSetUser(resList, LikeRecord.class.getMethod("getCreatorId"), LikeRecord.class.getMethod("setCreator", User.class));
            return new Result<List<LikeRecord>>(SUCCESS).setObject(resList);
        };
        callBacks.multiSetRedisIndexCallBack = (entityList, executor) -> {
            LikeRecord param = executor.param.paramEntity;
            Set<Tuple> indexSet = new HashSet<>();
            for (int i = 0; i < entityList.size(); ++i) {
                LikeRecord likeRecord = entityList.get(i);
                //formerSet里所有的value都加上likeRecordId，因为在允许newSet清空后再次点赞的情况下
                //对于同一个targetId可能有多个相同的creatorId，所以需要额外加上likeRecordId防止重复的被算作一条
                indexSet.add(new DefaultTuple(getRedisValue(likeRecord.getCreatorId(), likeRecord.getId()).getBytes(), (double) likeRecord.getCreateTime().getTime()));
            }
            String key = getFormerRedisSetKey(param.getTargetId(), param.getType());
            Object[] result = RedisUtil.addToZSetAndGetLastAndLength(redisTemplate, key, indexSet, Integer.MAX_VALUE, false);
            if (result == null) {
                return;
            }
            executor.param.newStartValue = (String) result[0];
            executor.param.newFrom = (int) (long) result[1];
            executor.param.newStartId = entityList.get(entityList.size() - 1).getId();
        };
        return callBacks;
    }

    /**
     * 获取某个目标对象的点赞列表
     *
     * @param param
     * @param operator
     * @return
     */
    public Result<List<LikeRecord>> readTargetLikeList(@NotNull LikeRecord param, Operator operator) {
        if (!param.isValidTargetListSelectParam()) {
            return new Result<List<LikeRecord>>(FAIL).setErrorCode(1010070202).setMessage("读取点赞列表失败，参数错误");
        }
        Result<Object> validateTokenResult = QueryTokenUtil.validateTokenAndSetQueryParam(param, 0, hmacKey);
        if (validateTokenResult.getState() == FAIL) {
            return new Result<List<LikeRecord>>(validateTokenResult);
        }
        Result<?> targetResult = getTarget(param, true, operator);
        if (targetResult.getState() == FAIL) {
            return new Result<List<LikeRecord>>(targetResult);
        }
        LikeRecord queryEntity = (LikeRecord) new LikeRecord().setQueryParam(new QueryParam());
        IndexExecutor<LikeRecord> indexExecutor = new IndexExecutor<>(param, queryEntity, null, indexExecutorCallBacks, operator);
        indexExecutor.param.initStartId = Integer.MAX_VALUE;
        indexExecutor.param.childNumOfParent = ((AbstractContent<?>) targetResult.getObject()).getLikeCnt();
        Result<List<LikeRecord>> resListResult = indexExecutor.doIndex();
        String newToken = QueryTokenUtil.generateQueryToken(param, resListResult.getObject(), queryEntity.getQueryParam(), 0, hmacKey);
        return resListResult.setAttachedToken(newToken);
    }

    /**
     * 点赞
     */
    public Result<Object> createLikeRecord(@NotNull LikeRecord likeRecord, Operator operator) {
        if (!likeRecord.isValidInsertParam()) {
            return new Result<Object>(FAIL).setErrorCode(1010070206).setMessage("点赞失败，参数错误");
        }
        if (!likeRecordValidator.checkCreatePermission(likeRecord, operator)) {
            return new Result<Object>(FAIL).setErrorCode(1010070212).setMessage("操作失败，权限错误");
        }
        likeRecord.setCreatorId(operator.getUserId());
        Result<?> targetResult = getTarget(likeRecord, false, operator);
        if (targetResult.getState() == FAIL) {
            return new Result<Object>(targetResult);
        }
        if (!addNewLikeRecord(likeRecord)) {
            return new Result<Object>(FAIL).setErrorCode(1010070209).setMessage("不能重复点赞");
        }
        if (likeRecord.getType().equals(LikeRecord.TYPE_POST)) {
            Post post = (Post) targetResult.getObject();
            postService.incrLikeCnt(post, 1);
            if (!post.getCreatorId().equals(operator.getUserId())) {
                noticeService.pushNotice(post.getId(), Notice.TYPE_LIKE_POST, post.getCreatorId());
            }
        } else if (likeRecord.getType().equals(LikeRecord.TYPE_CML1)) {
            CommentL1 comment = (CommentL1) targetResult.getObject();
            String likeSetKey = getNewRedisSetKey(comment.getId(), LikeRecord.TYPE_CML1);
            commentL1Service.incrLikeCnt(comment.getPostId(), comment.getId(), likeSetKey, comment.getLikeCnt(), 1);
            if (!comment.getCreatorId().equals(operator.getUserId())) {
                noticeService.pushNotice(comment.getId(), Notice.TYPE_LIKE_CML1, comment.getCreatorId());
            }
        } else if (likeRecord.getType().equals(LikeRecord.TYPE_CML2)) {
            CommentL2 comment = (CommentL2) targetResult.getObject();
            String likeSetKey = getNewRedisSetKey(comment.getId(), LikeRecord.TYPE_CML2);
            commentL2Service.incrLikeCnt(comment.getL1Id(), comment.getId(), likeSetKey, comment.getLikeCnt(), 1);
            if (!comment.getCreatorId().equals(operator.getUserId())) {
                noticeService.pushNotice(comment.getId(), Notice.TYPE_LIKE_CML2, comment.getCreatorId());
            }
        }
        return new Result<Object>(Result.SUCCESS);
    }

    /**
     * 取消点赞
     */
    public Result<Object> deleteLikeRecord(@NotNull LikeRecord likeRecord, Operator operator) {
        if (!likeRecord.isValidDeleteParam()) {
            return new Result<Object>(FAIL).setErrorCode(1010070207).setMessage("取消点赞失败，参数错误");
        }
        if (!likeRecordValidator.checkDeletePermission(likeRecord, operator)) {
            return new Result<Object>(FAIL).setErrorCode(1010070211).setMessage("操作失败，权限错误");
        }
        likeRecord.setCreatorId(operator.getUserId());
        Result<?> targetResult = getTarget(likeRecord, false, operator);
        if (targetResult.getState() == FAIL) {
            return new Result<Object>(targetResult);
        }
        if (!removeLikeRecord(likeRecord)) {
            return new Result<Object>(FAIL).setErrorCode(1010070210).setMessage("没有该条点赞记录");
        }
        if (likeRecord.getType().equals(LikeRecord.TYPE_POST)) {
            Post post = (Post) targetResult.getObject();
            postService.incrLikeCnt(post, -1);
        } else if (likeRecord.getType().equals(LikeRecord.TYPE_CML1)) {
            CommentL1 comment = (CommentL1) targetResult.getObject();
            commentL1Service.incrLikeCnt(comment.getPostId(), comment.getId(), null, 0, -1);
        } else if (likeRecord.getType().equals(LikeRecord.TYPE_CML2)) {
            CommentL2 comment = (CommentL2) targetResult.getObject();
            commentL2Service.incrLikeCnt(comment.getL1Id(), comment.getId(), null, 0, -1);
        }
        return new Result<Object>(Result.SUCCESS);
    }

    /**
     * 每天将Redis中的点赞记录刷入数据库
     */
    @SuppressWarnings("unchecked")
    @Scheduled(cron = "0 50 2 * * ?")
    public void dailyFlushRedisLikeRecordToDb() {
        List<TmpEntry>[] tmpEntryList = new ArrayList[3];
        List<LikeRecord> likeRecordList = new ArrayList<>();
        for (int type = 0; type <= 2; ++type) {
            tmpEntryList[type] = new ArrayList<>();
            Set<String> likeSetKeys = RedisUtil.scanAllKeys(redisTemplate, getNewRedisSetKeyPrefix(type));
            redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, getFormerRedisSetKeyPrefix(type)));
            for (String key : likeSetKeys) {
                Integer targetId = Integer.valueOf(key.split(":")[1]);
                List<Object> resList = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                    connection.zRangeWithScores(key.getBytes(), 0, -1);
                    connection.del(key.getBytes());
                    return null;
                });
                Set<TypedTuple<String>> tupleSet = (Set<TypedTuple<String>>) resList.get(0);
                int cnt = 0;
                for (TypedTuple<String> tuple : tupleSet) {
                    RedisValue v = parseRedisValue(tuple.getValue());
                    Date createTime = new Date(Math.round(tuple.getScore()));
                    likeRecordList.add((LikeRecord) new LikeRecord().setTargetId(targetId).setCreatorId(Integer.valueOf(v.creatorId)).setType(type).setCreateTime(createTime));
                    ++cnt;
                }
                tmpEntryList[type].add(new TmpEntry(targetId, cnt));
            }
        }
        if (likeRecordList.size() != 0) {
            // 存入数据库
            insertList(likeRecordList);
        }
        commonService.batchUpdate("lpt_post", "post_id", "post_like_cnt", CommonService.OPS_INCR_BY, tmpEntryList[0]);
        commonService.batchUpdate("lpt_comment_l1", "comment_l1_id", "comment_l1_like_cnt", CommonService.OPS_INCR_BY, tmpEntryList[1]);
        commonService.batchUpdate("lpt_comment_l2", "comment_l2_id", "comment_l2_like_cnt", CommonService.OPS_INCR_BY, tmpEntryList[2]);
        log.info("所有对象的点赞记录写入数据库");
        // 更新hmac的key
        hmacKey = CryptUtil.randUrlSafeStr(64, true);
    }

    private class RedisValue {
        /**
         * 点赞者ID(用户ID)
         */
        Integer creatorId;
        /**
         * 数据库点赞记录ID，无业务含义
         */
        Integer likeRecordId;

        RedisValue(Integer creatorId, Integer likeRecordId) {
            this.creatorId = creatorId;
            this.likeRecordId = likeRecordId;
        }
    }

}
