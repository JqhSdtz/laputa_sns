package com.laputa.laputa_sns.helper;

import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.executor.IndexExecutor;
import com.laputa.laputa_sns.model.entity.*;
import com.laputa.laputa_sns.service.*;
import com.laputa.laputa_sns.util.RedisUtil;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.*;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * @author JQH
 * @since 下午 1:41 20/03/14
 */
public class CommentServiceHelper<T extends AbstractContent<T>> {

    public static final int POPULAR = 0;
    public static final int LATEST = 1;

    private final String[] typeStr = {"P", "L"};

    private final int commentLevel;
    private final QueryHelper<T> queryHelper;
    private final RedisHelper<T> redisHelper;
    private final IndexExecutor.CallBacks<T> indexExecutorCallBacks;
    private final StringRedisTemplate redisTemplate;
    private final LikeRecordService likeRecordService;
    private final CommonService commonService;
    private final UserService userService;
    private final BaseService<?, T> commentService;
    private final Class<T> commentClass;
    private static final DefaultRedisScript<Long> incrLikeCntScript = new DefaultRedisScript<>(
            "local delta = tonumber(ARGV[3])\n" + "local zscore = redis.call('zscore', KEYS[1], ARGV[1])\n" + "if (zscore ~= nil and zscore ~= false) then\n" +
                    "\tredis.call('zincrby', KEYS[1], delta, ARGV[1])\n" + "\treturn 0\n" + "end\n" + "if (delta > 0) then\n" +
                    "\tlocal zrange = redis.call('zrange', KEYS[1], 0, 0, 'WITHSCORES')\n" + "\tif (#zrange == 0) then \n" + "\t\treturn -1\n" +
                    "\tend\n" + "\tlocal score = tonumber(ARGV[2]) + redis.call('zcard', KEYS[2])\n" + "\tif (score > tonumber(zrange[2])) then\n" + "\t\tredis.call('zadd', KEYS[1], score, ARGV[1])\n" +
                    "\t\treturn 1\n" + "\telse\n" + "\t\treturn -1\n" + "\tend\n" + "end", Long.class);

    private final String redisIndexKeyPrefix;

    public CommentServiceHelper(int commentLevel, QueryHelper<T> queryHelper, RedisHelper<T> redisHelper, StringRedisTemplate redisTemplate, LikeRecordService likeRecordService, CommonService commonService, UserService userService, BaseService<?, T> commentService, Class<T> commentClass) {
        this.commentLevel = commentLevel;
        this.queryHelper = queryHelper;
        this.redisHelper = redisHelper;
        this.commentService = commentService;
        this.indexExecutorCallBacks = initIndexExecutorCallBacks();
        this.redisTemplate = redisTemplate;
        this.likeRecordService = likeRecordService;
        this.commonService = commonService;
        this.userService = userService;
        this.commentClass = commentClass;
        redisIndexKeyPrefix = commentLevel == 1 ? RedisPrefix.CML1_INDEX_OF_POST : RedisPrefix.CML2_INDEX_OF_CML1;
    }

    @NotNull
    public String getRedisIndexZSetKey(int parentId, int type) {
        return redisIndexKeyPrefix + "/" + typeStr[type] + ":" + parentId;
    }

    @NotNull
    public String getRedisIndexZSetKey(String str, int type) {
        return redisIndexKeyPrefix + "/" + typeStr[type] + ":" + str;
    }

    public void incrLikeCnt(int parentId, int commentId, String likeSetKey, long oriValue, int delta) {
        String popZSetKey = getRedisIndexZSetKey(parentId, POPULAR);
        double score = RedisUtil.genScore(oriValue, commentId);
        long res = redisTemplate.execute(incrLikeCntScript, Arrays.asList(popZSetKey, likeSetKey), String.valueOf(commentId), String.valueOf(score), String.valueOf(delta));
        if (res == 1) {
            if (commentLevel == 1)
                ((CommentL1Service) commentService).setPopularIndexFlag(commentId, 1);
            else
                ((CommentL2Service) commentService).setPopularIndexFlag(commentId, 1);
        }
    }

    private List<TypedTuple<String>> getRedisCommentIndex(Integer parentId, @NotNull QueryParam queryParam, int type) {
        return RedisUtil.readIndex(redisTemplate, getRedisIndexZSetKey(parentId, type), queryParam, type == LATEST, type == POPULAR);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public List<Set<TypedTuple<String>>> multiGetRedisCommentIndex(List<Integer> parentIdList, int num, int type) {
        final int end = num - 1;
        List<Object> resList = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            for (int i = 0; i < parentIdList.size(); ++i) {
                byte[] key = getRedisIndexZSetKey(parentIdList.get(i), type).getBytes();
                connection.zRevRangeWithScores(key, 0, end);
            }
            return null;
        });
        return (List<Set<TypedTuple<String>>>) (List<?>) resList;
    }

    public void addNewToRedisIndex(@NotNull T comment, int type) {
        if (type == LATEST)
            comment.setCreateTime(new Date());
        redisTemplate.opsForZSet().add(getRedisIndexZSetKey(comment.getParentId(), type), String.valueOf(comment.getId()), genScore(comment, type));
    }

    public void multiSetIndexedFlag(@NotNull List<T> entityList, int type, Integer flag) {
        List<TmpEntry> entryList = new ArrayList<>(entityList.size());
        for (int i = 0; i < entityList.size(); ++i)
            entryList.add(new TmpEntry(entityList.get(i).getId(), flag));
        String tableName = commentLevel == 1 ? "lpt_comment_l1" : "lpt_comment_l2";
        String tablePrefix = commentLevel == 1 ? "comment_l1_" : "comment_l2_";
        commonService.batchUpdate(tableName, tablePrefix + "id", tablePrefix + (type == POPULAR ? "p" : "l") + "_indexed_flag", CommonService.OPS_COPY, entryList);
    }

    @NotNull
    private void multiSetRedisCommentIndexAndGetLast(Integer parentId, @NotNull List<T> commentList, IndexExecutor<T>.Param param) {
        Set<Tuple> indexSet = new HashSet<>();
        for (int i = 0; i < commentList.size(); ++i) {
            T comment = commentList.get(i);
            indexSet.add(new DefaultTuple(String.valueOf(comment.getId()).getBytes(), genScore(comment, param.type)));
        }
        Object[] res = RedisUtil.addToZSetAndGetLastAndLength(redisTemplate, getRedisIndexZSetKey(parentId, param.type), indexSet, Integer.MAX_VALUE, false);
        if (res == null)
            return;
        multiSetIndexedFlag(commentList, param.type, 1);
        param.newStartId = Integer.valueOf((String) res[0]);
        param.newFrom = (int) (long) res[1];
    }

    /**
     * MySQL中取出来的结果相同的likeCnt是按id降序排序的，而redis中是按字典序处理相同值，所以在score加个后缀
     */
    public double genScore(@NotNull T comment, int type) {
        long value = type == POPULAR ? comment.getLikeCnt() : comment.getCreateTime().getTime();
        return RedisUtil.genScore(value, comment.getId());
    }

    private void removeRedisCommentIndex(Integer parentId, Integer commentId) {
        String popularSetKey = getRedisIndexZSetKey(parentId, POPULAR);
        String commentKey = String.valueOf(commentId);
        if (commentLevel == 2)
            redisTemplate.opsForZSet().remove(popularSetKey, commentKey);
        else {
            byte[] latestSetRawKey = getRedisIndexZSetKey(parentId, LATEST).getBytes();
            byte[] popularSetRawKey = popularSetKey.getBytes();
            byte[] commentRawKey = commentKey.getBytes();
            redisTemplate.execute(connection -> {
                connection.zRem(latestSetRawKey, commentRawKey);
                connection.zRem(popularSetRawKey, commentRawKey);
                return null;
            }, false, true);
        }
    }

    public void removeRedisComment(Integer parentId, Integer commentId) {
        redisHelper.removeEntity(commentId);
        removeRedisCommentIndex(parentId, commentId);
    }

    private Result<List<T>> getIndexFromDB(T comment, int type) {
        if (commentLevel == 2)
            type = POPULAR;
        String orderBy = type == LATEST ? "create_time" : "like_cnt";
        comment.getQueryParam().setIsPaged(1).setOrderBy(orderBy).setOrderDir("desc").setQueryType(QueryParam.FULL);
        comment.getQueryParam().setQueryNotIndexed(type == POPULAR ? 'p' : 'l');
        int oriFrom = comment.getQueryParam().getFrom();
        comment.getQueryParam().setFrom(0);
//        if ("create_time".equals(comment.getQueryParam().getOrderBy()) && !comment.getQueryParam().getStartId().equals(0))
//            comment.getQueryParam().setUseStartIdAtSql(1);//使用notIndexed就不用startId
        List<T> commentList = commentService.selectList(comment);
        comment.getQueryParam().setFrom(oriFrom);
        if (commentList == null)
            return new Result<List<T>>(FAIL).setErrorCode(1010170101).setMessage("数据库操作失败");
        redisHelper.multiSetAndRefreshEntity(commentList, null, true);
        //因为ZSet中保存的是完整的likeCnt的值，但是value中保存的是原始的likeCnt的值，所以设置value后，加入ZSet前要获取完整的likeCnt
        likeRecordService.multiSetLikeCnt(commentList, commentLevel == 1 ? LikeRecord.TYPE_CML1 : LikeRecord.TYPE_CML2);
        return new Result<List<T>>(SUCCESS).setObject(commentList);
    }

    private void setCreator(@NotNull T entity, Operator operator) {
        Result<User> userResult = userService.readUserWithAllFalse(entity.getCreatorId(), operator);
        if (userResult.getState() == SUCCESS)
            entity.setCreator(userResult.getObject());
    }

    public Result<T> readComment(Integer commentId, @NotNull T queryEntity, boolean withCreator, boolean withCounter, boolean withContent, boolean withIsLikedByViewer, Operator operator) {
        queryEntity.setId(commentId);
        Result<T> commentResult = queryHelper.readEntity(queryEntity, withContent);
        if (commentResult.getState() == FAIL)
            return commentResult;
        T resComment = commentResult.getObject();
        int likeRecordType = commentLevel == 1 ? LikeRecord.TYPE_CML1 : LikeRecord.TYPE_CML2;
        if (withCreator)
            setCreator(resComment, operator);
        if (withCounter)
            likeRecordService.setLikeCnt(resComment, likeRecordType);
        if (withIsLikedByViewer) {
            boolean res = likeRecordService.existLikeRecordInRedis((LikeRecord) new LikeRecord().setTargetId(commentId).setCreatorId(operator.getUserId()).setType(likeRecordType));
            resComment.setLikedByViewer(res);
        }
        return new Result<T>(SUCCESS).setObject(resComment);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private IndexExecutor.CallBacks<T> initIndexExecutorCallBacks() {
        IndexExecutor.CallBacks<T> callBacks = new IndexExecutor.CallBacks<>();
        callBacks.getIdListCallBack = (executor) -> {
            IndexExecutor<T>.Param param = executor.param;
            int parentId = ((T) param.paramEntity).getParentId();
            List<ZSetOperations.TypedTuple<String>> indexList = getRedisCommentIndex(parentId, param.paramEntity.getQueryParam(), param.type);
            List<Integer> idList = null;
            if (indexList != null) {
                idList = new ArrayList<>(indexList.size());
                for (int i = 0; i < indexList.size(); ++i)
                    idList.add(Integer.valueOf(indexList.get(i).getValue()));
            }
            executor.param.indexSetList = indexList;
            param.idList = idList;
        };
        callBacks.multiReadEntityCallBack = (executor) -> {
            IndexExecutor<T>.Param param = executor.param;
            Result<List<T>> commentListResult = queryHelper.multiReadEntity(param.idList, true, (T) param.queryEntity);
            if (commentListResult.getState() == FAIL)
                return commentListResult;
            List<T> commentList = commentListResult.getObject();
            if (param.type == POPULAR)//只有popular的likeCnt是可以直接从索引的zSet中获取的
                for (int i = 0; i < commentList.size(); ++i) {
                    double value = ((ZSetOperations.TypedTuple<String>) param.indexSetList.get(i)).getScore();
                    commentList.get(i).setLikeCnt((long) value);//因为value作为double值的时候小数部分是id
                }
            else
                likeRecordService.multiSetLikeCnt(commentList, commentLevel == 1 ? LikeRecord.TYPE_CML1 : LikeRecord.TYPE_CML2);
            return commentListResult;
        };
        callBacks.getDBListCallBack = (executor) -> getIndexFromDB((T) executor.param.paramEntity, executor.param.type);
        callBacks.multiSetRedisIndexCallBack = (entityList, executor) -> multiSetRedisCommentIndexAndGetLast(((T) executor.param.paramEntity).getParentId(), entityList, executor.param);
        callBacks.readOneEntityCallBack = (id, executor) -> {
            if (commentLevel == 1) {
                return (Result<T>) ((CommentL1Service) commentService).readComment(id, true, false, true, false, executor.param.operator);
            } else {
                return (Result<T>) ((CommentL2Service) commentService).readComment(id, true, false, true, false, executor.param.operator);
            }
        };
        return callBacks;
    }

    /**
     * 会更改comment.queryParam.startId
     */
    @SneakyThrows
    public Result<List<T>> readIndexComment(@NotNull T comment, T queryEntity, int type, Operator operator) {
        if (commentLevel == 2)//二级评论只能按热度排序
            type = POPULAR;
        IndexExecutor<T> indexExecutor = new IndexExecutor<>(comment, queryEntity, type, indexExecutorCallBacks, operator);
        indexExecutor.param.childNumOfParent = comment.getParent().getChildNum();
        if (commentLevel == 1)
            indexExecutor.param.topId = ((CommentL1) comment).getPost().getTopCommentId();
        Result<List<T>> commentListResult = indexExecutor.doIndex();
        if (commentListResult.getState() == FAIL)
            return commentListResult;
        List<T> commentList = commentListResult.getObject();
        userService.multiSetUser(commentList, commentClass.getMethod("getCreatorId"), commentClass.getMethod("setCreator", User.class));
        int likeRecordType = commentLevel == 1 ? LikeRecord.TYPE_CML1 : LikeRecord.TYPE_CML2;
        likeRecordService.multiSetIsLikedByViewer(commentList, likeRecordType, operator.getUserId());
        return new Result<List<T>>(SUCCESS).setObject(commentList);
    }

}
