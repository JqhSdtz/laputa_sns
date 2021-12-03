package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.dao.CommentL1Dao;
import com.laputa.laputa_sns.helper.CommentServiceHelper;
import com.laputa.laputa_sns.helper.QueryHelper;
import com.laputa.laputa_sns.helper.RedisHelper;
import com.laputa.laputa_sns.model.entity.*;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.QueryTokenUtil;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.validator.CommentL1Validator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;
import static com.laputa.laputa_sns.helper.CommentServiceHelper.POPULAR;
import static com.laputa.laputa_sns.helper.CommentServiceHelper.LATEST;

/**
 * 一级评论服务
 * @author JQH
 * @since 下午 12:25 20/02/27
 */

@Slf4j
@EnableScheduling
@Service
public class CommentL1Service extends BaseService<CommentL1Dao, CommentL1> {

    private final PostService postService;
    private final CommonService commonService;
    private final CommentL2Service commentL2Service;
    private final LikeRecordService likeRecordService;
    private final NoticeService noticeService;
    private final AdminOpsService adminOpsService;
    private final CommentL1Validator commentL1Validator;
    private final StringRedisTemplate redisTemplate;
    private final RedisHelper<CommentL1> redisHelper;
    private final QueryHelper<CommentL1> queryHelper;
    private final CommentServiceHelper<CommentL1> serviceHelper;
    private final ObjectMapper fullObjectMapper = new ObjectMapper();

    private String hmacKey;

    public CommentL1Service(UserService userService, LikeRecordService likeRecordService, PostService postService, CommentL1Validator commentL1Validator, StringRedisTemplate redisTemplate, @NotNull Environment environment, CommonService commonService, @Lazy CommentL2Service commentL2Service, NoticeService noticeService, AdminOpsService adminOpsService) {
        this.postService = postService;
        this.likeRecordService = likeRecordService;
        this.commentL1Validator = commentL1Validator;
        this.redisTemplate = redisTemplate;
        this.commonService = commonService;
        this.commentL2Service = commentL2Service;
        this.noticeService = noticeService;
        this.adminOpsService = adminOpsService;
        this.hmacKey = CryptUtil.randUrlSafeStr(64, true);
        ObjectMapper basicMapper = new ObjectMapper();
        basicMapper.setFilterProvider(new SimpleFilterProvider().addFilter("CommentL1Filter", SimpleBeanPropertyFilter
                .filterOutAllExcept("post_id", "l2_cnt", "like_cnt", "type", "creator_id", "poster_rep_cnt", "create_time")));
        ObjectMapper contentMapper = new ObjectMapper();
        contentMapper.setFilterProvider(new SimpleFilterProvider().addFilter("CommentL1Filter", SimpleBeanPropertyFilter
                .filterOutAllExcept("content", "raw_img")));
        this.fullObjectMapper.setFilterProvider(new SimpleFilterProvider()
                .addFilter("CommentL1Filter", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("UserFilter", SimpleBeanPropertyFilter.filterOutAllExcept("nick_name", "raw_avatar", "type", "state")));
        int basicRedisTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.comment.l1.basic"));
        int contentRedisTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.comment.l1.content"));
        this.redisHelper = new RedisHelper<>(RedisPrefix.CML1_BASIC, basicMapper, basicRedisTimeOut, RedisPrefix.CML1_CONTENT, contentMapper, contentRedisTimeOut, RedisPrefix.CML1_COUNTER, CommentL1.class, redisTemplate);
        this.queryHelper = new QueryHelper<>(this, redisHelper);
        this.serviceHelper = new CommentServiceHelper<>(1, queryHelper, redisHelper, redisTemplate, likeRecordService, commonService, userService, this, CommentL1.class);
    }

    /**
     * 更新一级评论的数量相关的信息
     * @param commentId 评论ID
     * @param l2Delta 二级评论的增量
     * @param pcmDelta 二级评论中发帖人评论的增量
     */
    public void updateCounters(Integer commentId, Long l2Delta, Long pcmDelta) {
        Map<String, Long> map = new HashMap<>(2);
        if (l2Delta != null)
            map.put("l2", l2Delta);
        if (pcmDelta != null)
            map.put("pcm", pcmDelta);
        redisHelper.updateCounters(commentId, map);
    }

    /**
     * 增加点赞数量
     * @param parentId
     * @param commentId
     * @param likeSetKey redis中该评论点赞记录集合的key
     * @param oriValue
     * @param delta
     */
    public void incrLikeCnt(int parentId, int commentId, String likeSetKey, long oriValue, int delta) {
        serviceHelper.incrLikeCnt(parentId, commentId, likeSetKey, oriValue, delta);
    }

    /**
     * 设置一级评论表中的热门索引标志字段，即是否已经处于热门索引，防止从数据库中选取热门索引时重复选取
     * @param id
     * @param value
     * @return
     */
    public int setPopularIndexFlag(int id, int value) {
        return dao.setPopularIndexFlag(id, value);
    }

    private void setCounter(@NotNull CommentL1 comment) {
        Long[] v = redisHelper.getRedisCounterCnt(comment.getId(), "l2", "pcm");
        if (v == null)
            return;
        if (v[0] != null)
            comment.setL2Cnt(comment.getL2Cnt() + v[0]);
        if (v[1] != null)
            comment.setPosterRepCnt(comment.getPosterRepCnt() + v[1]);
    }

    private void multiSetCounter(@NotNull List<CommentL1> commentList) {
        List<List<String>> vList = redisHelper.multiGetRedisCounterCnt(commentList, "l2", "pcm");
        for (int i = 0; i < commentList.size(); ++i) {
            CommentL1 comment = commentList.get(i);
            if (comment == null)
                continue;
            List<String> v = vList.get(i);
            if (v.get(0) != null)
                comment.setL2Cnt(comment.getL2Cnt() + Integer.valueOf(v.get(0)));
            if (v.get(1) != null)
                comment.setPosterRepCnt(comment.getPosterRepCnt() + Integer.valueOf(v.get(1)));
        }
    }

    public boolean existComment(CommentL1 comment) {
        return queryHelper.existEntity(comment);
    }

    private void multiSetPreviewL2List(CommentL1 commentL1, @NotNull List<CommentL1> l1List, Operator operator) {
        List<Integer> idList = new ArrayList<>(l1List.size());
        for (int i = 0; i < l1List.size(); ++i)
            if (!l1List.get(i).getL2Cnt().equals(0L))
                idList.add(l1List.get(i).getId());
        List<CommentL2> l2List = commentL2Service.multiReadPreviewIndex(commentL1, idList, operator);
        HashMap<Integer, List<CommentL2>> l2OfL1Map = new HashMap<>(l1List.size());
        for (int i = 0; i < l2List.size(); ++i) {
            CommentL2 comment = l2List.get(i);
            List<CommentL2> l2Ofl1List;
            if (!l2OfL1Map.containsKey(comment.getL1Id())) {
                l2Ofl1List = new ArrayList<>();
                l2OfL1Map.put(comment.getL1Id(), l2Ofl1List);
            } else
                l2Ofl1List = l2OfL1Map.get(comment.getL1Id());
            l2Ofl1List.add(comment);
        }
        for (int i = 0; i < l1List.size(); ++i)
            l1List.get(i).setPreviewL2List(l2OfL1Map.get(l1List.get(i).getId()));
    }

    public Result<CommentL1> readCommentWithAllFalse(Integer commentId, Operator operator) {
        return readComment(commentId, false, false, false, false, operator);
    }

    public Result<CommentL1> readCommentWithContent(Integer commentId, Operator operator) {
        return readComment(commentId, false, false, true, false, operator);
    }

    public Result<CommentL1> readCommentWithCounter(Integer commentId, Operator operator) {
        return readComment(commentId, false, true, false, false, operator);
    }

    public Result<List<CommentL1>> multiReadCommentWithContentAndCounter(List<Integer> idList) {
        Result<List<CommentL1>> result = queryHelper.multiReadEntity(idList, true, new CommentL1());
        if (result.getState() == SUCCESS) {
            likeRecordService.multiSetLikeCnt(result.getObject(), LikeRecord.TYPE_CML1);
            multiSetCounter(result.getObject());
        }
        return result;
    }

    public Result<CommentL1> readComment(Integer commentId, boolean withCreator, boolean withCounter, boolean withContent, boolean withIsLikedByViewer, Operator operator) {
        Result<CommentL1> commentResult = serviceHelper.readComment(commentId, new CommentL1(), withCreator, withCounter, withContent, withIsLikedByViewer, operator);
        if (commentResult.getState() == FAIL)
            return commentResult;
        CommentL1 resComment = commentResult.getObject();
        if (withCounter)
            setCounter(resComment);
        return new Result<CommentL1>(SUCCESS).setObject(resComment);
    }

    /**
     * 读取指定帖子的评论
     */
    public Result<List<CommentL1>> readIndexCommentL1List(@NotNull CommentL1 comment, int type, boolean withPreviewL2List, Operator operator) {
        if (!comment.isValidReadIndexParam(true))
            return new Result<List<CommentL1>>(FAIL).setErrorCode(1010080203).setMessage("操作错误，参数不合法");
        Result<Object> validateTokenResult = QueryTokenUtil.validateTokenAndSetQueryParam(comment, type, hmacKey);
        if (validateTokenResult.getState() == FAIL)
            return new Result<List<CommentL1>>(validateTokenResult);
        //在serviceHelper.readIndexComment中需要判断post的comment_cnt，所以需要带counter，rights是留给设置置顶权限用的
        Result<Post> postResult = postService.readPostWithCounter(comment.getPostId(), operator);
        if (postResult.getState() == FAIL)
            return new Result<List<CommentL1>>(postResult);
        comment.setPost(postResult.getObject());
        CommentL1 queryEntity = (CommentL1) new CommentL1().setQueryParam(new QueryParam());
        Result<List<CommentL1>> commentListResult = serviceHelper.readIndexComment(comment, queryEntity, type, operator);
        if (commentListResult.getState() == FAIL)
            return commentListResult;
        List<CommentL1> commentList = commentListResult.getObject();
        multiSetCounter(commentList);
        if (withPreviewL2List)
            multiSetPreviewL2List(comment, commentList, operator);
        commentL1Validator.multiSetRights(comment, commentList, operator);
        String newToken = QueryTokenUtil.generateQueryToken(comment, commentList, queryEntity.getQueryParam(), type, hmacKey);
        return new Result<List<CommentL1>>(SUCCESS).setObject(commentList).setAttachedToken(newToken);
    }

    /**
     * 发表评论
     */
    public Result<Integer> createComment(@NotNull CommentL1 comment, Operator operator) {
        if (!comment.isValidInsertParam())
            return new Result<Integer>(FAIL).setErrorCode(1010080204).setMessage("操作错误，参数不合法");
        if (!commentL1Validator.checkCreatePermission(comment, operator))
            return new Result<Integer>(FAIL).setErrorCode(1010080205).setMessage("操作失败，权限错误");
        Result<Post> postResult = postService.readPostWithAllFalse(comment.getPostId(), operator);
        if (postResult.getState() == FAIL)
            return new Result<Integer>(postResult);
        Post post = postResult.getObject();
        comment.setCreator(operator.getUser()).setLIndexedFlag(true);
        int res = insertOne(comment);
        if (res == -1)
            return new Result<Integer>(FAIL).setErrorCode(1010080106).setMessage("数据库操作失败");
        comment.setLikeCnt(0L).setL2Cnt(0L);
        postService.updateCommentCnt(comment.getPostId(), 1L);
        serviceHelper.addNewToRedisIndex(comment, LATEST);//新添加的comment，latest索引的标志默认为1，当然这也要求新创建的comment必须加入latest索引中
        if (!post.getCreatorId().equals(operator.getUserId()))
            noticeService.pushNotice(post.getId(), Notice.TYPE_CML1_OF_POST, post.getCreatorId());
        return new Result<Integer>(SUCCESS).setObject(comment.getId());
    }

    /**
     * 删除评论
     */
    @SneakyThrows
    public Result<Object> deleteComment(@NotNull CommentL1 param, Operator operator) {
        if (!param.isValidDeleteParam())
            return new Result<Object>(FAIL).setErrorCode(1010080207).setMessage("操作错误，参数不合法");
        Result<CommentL1> commentResult = readCommentWithContent(param.getId(), operator);
        if (commentResult.getState() == FAIL)
            return new Result<Object>(commentResult);
        CommentL1 comment = commentResult.getObject();
        Result<Post> postResult = postService.readPostWithAllFalse(comment.getPostId(), operator);
        if (postResult.getState() == FAIL)
            return new Result<Object>(postResult);
        comment.setPost(postResult.getObject());
        if (!commentL1Validator.checkDeletePermission(comment, operator))
            return new Result<Object>(FAIL).setErrorCode(1010080208).setMessage("操作失败，权限错误");
        boolean isAdminOp = !comment.getCreatorId().equals(operator.getUserId());
        if (isAdminOp && !param.isValidOpComment())
            return new Result<Object>(FAIL).setErrorCode(1010080210).setMessage("操作错误，操作原因字数在5-256");
        int res = deleteOne(comment);
        if (res == 0)
            return new Result<Object>(FAIL).setErrorCode(1010080209).setMessage("数据库操作失败");
        serviceHelper.removeRedisComment(comment.getPostId(), comment.getId());
        postService.updateCommentCnt(comment.getPostId(), -1L);
        if (isAdminOp) {
            AdminOpsRecord record = new AdminOpsRecord();
            record.setTargetId(comment.getCreatorId()).setTarget(comment).setDesc(fullObjectMapper.writeValueAsString(comment)).setOpComment(param.getOpComment()).setType(AdminOpsRecord.TYPE_DELETE_CML1);
            adminOpsService.createAdminOpsRecord(record, operator);
        }
        return new Result<Object>(Result.SUCCESS);
    }

    public String correctCounters() {
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getCounterKey("*")));
        int r1 = dao.correctL2Cnt();
        if (r1 == 0)
            return "一级评论数据校正错误，请重新校正";
        int r2 = dao.correctPosterRepCnt();
        if (r2 == 0)
            return "一级评论数据校正错误，请重新校正";
        int r3 = dao.correctLikeCnt();
        if (r3 == 0)
            return "一级评论数据校正错误，请重新校正";
        return "一级评论的二级评论数校正" + r1 + "条数据;一级评论的发帖人回复数" + r2 + "条数据;一级评论的点赞数" + r3 + "条数据";
    }

    /**
     * 每天将Redis中的一级评论的二级评论的数量记录刷入数据库
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void dailyFlushRedisToDB() {
        List<TmpEntry>[] cntLists = redisHelper.flushRedisCounter("l2", "pcm");
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getBasicKey("*")));
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getContentKey("*")));
        commonService.batchUpdate("lpt_comment_l1", "comment_l1_id", "comment_l1_l2_cnt", CommonService.OPS_INCR_BY, cntLists[0]);
        commonService.batchUpdate("lpt_comment_l1", "comment_l1_id", "comment_l1_poster_rep_cnt", CommonService.OPS_INCR_BY, cntLists[1]);
        log.info("一级评论的二级评论数量和发帖人回复的评论数量写入数据库");
        commonService.clearIndexedFlag("lpt_comment_l1", "comment_l1_id", "comment_l1_p_indexed_flag", "comment_l1_l_indexed_flag");
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, serviceHelper.getRedisIndexZSetKey("*", POPULAR)));
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, serviceHelper.getRedisIndexZSetKey("*", LATEST)));
        log.info("帖子的一级评论索引清除");
        hmacKey = CryptUtil.randUrlSafeStr(64, true);//更新hmac的key
    }

}
