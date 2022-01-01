package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.dao.CommentL2Dao;
import com.laputa.laputa_sns.helper.CommentServiceHelper;
import com.laputa.laputa_sns.helper.QueryHelper;
import com.laputa.laputa_sns.helper.RedisHelper;
import com.laputa.laputa_sns.model.entity.*;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.QueryTokenUtil;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.validator.CommentL2Validator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;
import static com.laputa.laputa_sns.helper.CommentServiceHelper.POPULAR;

/**
 * 二级评论服务
 *
 * @author JQH
 * @since 下午 1:46 20/02/29
 */

@Slf4j
@EnableScheduling
@Service
public class CommentL2Service extends BaseService<CommentL2Dao, CommentL2> {

    private final PostService postService;
    private final CommentL1Service commentL1Service;
    private final CommonService commonService;
    private final UserService userService;
    private final LikeRecordService likeRecordService;
    private final NoticeService noticeService;
    private final AdminOpsService adminOpsService;
    private final CommentL2Validator commentL2Validator;
    private final StringRedisTemplate redisTemplate;
    private final RedisHelper<CommentL2> redisHelper;
    private final QueryHelper<CommentL2> queryHelper;
    private final CommentServiceHelper<CommentL2> serviceHelper;
    private final ObjectMapper fullObjectMapper = new ObjectMapper();

    private String hmacKey;

    /**
     * 在显示一级评论时附带显示的二级评论的数量
     */
    @Value("${preview-cml2-num}")
    private int previewCml2Num;

    public CommentL2Service(UserService userService, LikeRecordService likeRecordService, PostService postService, CommentL1Service commentL1Service, CommentL2Validator commentL2Validator, StringRedisTemplate redisTemplate, @NotNull Environment environment, CommonService commonService, LikeRecordService likeRecordService1, NoticeService noticeService, AdminOpsService adminOpsService) {
        this.postService = postService;
        this.commentL1Service = commentL1Service;
        this.commentL2Validator = commentL2Validator;
        this.redisTemplate = redisTemplate;
        this.commonService = commonService;
        this.userService = userService;
        this.likeRecordService = likeRecordService1;
        this.noticeService = noticeService;
        this.adminOpsService = adminOpsService;
        this.hmacKey = CryptUtil.randUrlSafeStr(64, true);
        ObjectMapper basicMapper = new ObjectMapper();
        basicMapper.setFilterProvider(new SimpleFilterProvider().addFilter("CommentL2Filter", SimpleBeanPropertyFilter
                .filterOutAllExcept("l1_id", "type", "like_cnt", "post_id", "creator_id", "reply_to_id", "create_time")));
        ObjectMapper contentMapper = new ObjectMapper();
        contentMapper.setFilterProvider(new SimpleFilterProvider().addFilter("CommentL2Filter", SimpleBeanPropertyFilter
                .filterOutAllExcept("content", "raw_img")));
        this.fullObjectMapper.setFilterProvider(new SimpleFilterProvider()
                .addFilter("CommentL2Filter", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("UserFilter", SimpleBeanPropertyFilter.filterOutAllExcept("nick_name", "raw_avatar", "type", "state")));
        int basicRedisTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.comment.l2.basic"));
        int contentRedisTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.comment.l2.content"));
        this.redisHelper = new RedisHelper<>(RedisPrefix.CML2_BASIC, basicMapper, basicRedisTimeOut, RedisPrefix.CML2_CONTENT, contentMapper, contentRedisTimeOut, null, CommentL2.class, redisTemplate);
        this.queryHelper = new QueryHelper<>(this, redisHelper);
        this.serviceHelper = new CommentServiceHelper<>(2, queryHelper, redisHelper, redisTemplate, likeRecordService, commonService, userService, this, CommentL2.class);
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    private List<TmpEntry> selectTopNL2OfL1StrByMultiId(List<Integer> idList) {
        return dao.selectTopNL2OfL1StrByMultiId(idList);
    }

    public void incrLikeCnt(int parentId, int commentId, String likeSetKey, long oriValue, int delta) {
        serviceHelper.incrLikeCnt(parentId, commentId, likeSetKey, oriValue, delta);
    }

    public int setPopularIndexFlag(int id, int value) {
        return dao.setPopularIndexFlag(id, value);
    }

    /**
     * 设置预览的L2的索引，一次设置多个L1的L2索引
     */
    private void setMultiParentRedisCommentIndex(int l1ListSize, @NotNull List<CommentL2> indexList) {
        HashMap<Integer, Set<Tuple>> l2OfL1Map = new HashMap<>(l1ListSize);
        for (int i = 0; i < indexList.size(); ++i) {
            CommentL2 comment = indexList.get(i);
            Set<Tuple> tupleSet;
            if (!l2OfL1Map.containsKey(comment.getL1Id())) {
                tupleSet = new HashSet<>();
                l2OfL1Map.put(comment.getL1Id(), tupleSet);
            } else {
                tupleSet = l2OfL1Map.get(comment.getL1Id());
            }
            tupleSet.add(new DefaultTuple(String.valueOf(comment.getId()).getBytes(), serviceHelper.genScore(comment, POPULAR)));
        }
        redisTemplate.execute(connection -> {
            for (Map.Entry<Integer, Set<Tuple>> entry : l2OfL1Map.entrySet()) {
                connection.zAdd(serviceHelper.getRedisIndexZSetKey(entry.getKey(), POPULAR).getBytes(), entry.getValue());
            }
            return null;
        }, false, true);
        serviceHelper.multiSetIndexedFlag(indexList, POPULAR, 1);
    }

    public Result<List<CommentL2>> multiReadCommentWithContentAndCounter(List<Integer> idList) {
        Result<List<CommentL2>> result = queryHelper.multiReadEntity(idList, true, new CommentL2());
        if (result.getState() == SUCCESS) {
            likeRecordService.multiSetLikeCnt(result.getObject(), LikeRecord.TYPE_CML2);
        }
        return result;
    }

    /**
     * 这里也涉及到索引的读写，这是一次读多个L1的L2索引
     */
    @SneakyThrows
    public List<CommentL2> multiReadPreviewIndex(CommentL1 commentL1, List<Integer> l1IdList, Operator operator) {
        List<Set<TypedTuple<String>>> redisResIdList = serviceHelper.multiGetRedisCommentIndex(l1IdList, previewCml2Num, POPULAR);
        List<CommentL2> l2List = null;
        int len = 0;
        for (int i = 0; i < redisResIdList.size(); ++i) {
            len += redisResIdList.get(i).size();
        }
        List<Integer> dbL1IdList;
        if (len != 0) {
            // redis中包含部分或全部的目标索引
            dbL1IdList = new ArrayList<>();
            List<Integer> l2IdList = new ArrayList<>(len);
            HashMap<Integer, Long> likeCntMap = new HashMap<>(len);
            for (int i = 0; i < redisResIdList.size(); ++i) {
                if (redisResIdList.get(i).size() == 0) {
                    dbL1IdList.add(l1IdList.get(i));
                }
                for (TypedTuple<String> entry : redisResIdList.get(i)) {
                    Integer id = Integer.valueOf(entry.getValue());
                    l2IdList.add(id);
                    likeCntMap.put(id, (long) (double) entry.getScore());
                }
            }
            l2List = queryHelper.multiReadEntity(l2IdList, true, new CommentL2()).getObject();
            // 直接通过score设置likeCnt
            for (int i = 0; i < l2List.size(); ++i) {
                l2List.get(i).setLikeCnt(likeCntMap.get(l2List.get(i).getId()));
            }
        } else {
            dbL1IdList = l1IdList;
        }
        if (l2List == null) {
            l2List = new ArrayList<>();
        }
        List<CommentL2> dbL2List = null;
        // 需要查数据库
        if (dbL1IdList.size() != 0) {
            List<Integer> l2IdList = new ArrayList<>();
            @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming") List<TmpEntry> topNL2TmpList = selectTopNL2OfL1StrByMultiId(dbL1IdList);
            if (topNL2TmpList != null) {
                for (int i = 0; i < topNL2TmpList.size(); ++i) {
                    String tmpStr = (String) topNL2TmpList.get(i).getValue();
                    if (tmpStr != null) {
                        String[] ids = tmpStr.split(";");
                        for (int j = 0; j < ids.length; ++j) {
                            l2IdList.add(Integer.valueOf(ids[j]));
                        }
                    }
                }
            }
            dbL2List = queryHelper.multiReadEntity(l2IdList, true, new CommentL2()).getObject();
            // 从数据库中拿出来的需要设置likeCnt
            likeRecordService.multiSetLikeCnt(dbL2List, LikeRecord.TYPE_CML2);
            l2List.addAll(dbL2List);
        }
        // 这一句需要在setMultiParentRedisCommentIndex之前，保证添加到Redis中的likeCnt是正确的
        // serviceHelper.multiSetLikeCnt(l2List);
        if (dbL2List != null) {
            setMultiParentRedisCommentIndex(dbL1IdList.size(), dbL2List);
        }
        commentL2Validator.multiSetRights(new CommentL2().setL1(commentL1), l2List, operator);
        userService.multiSetUser(l2List, CommentL2.class.getMethod("getCreatorId"), CommentL2.class.getMethod("setCreator", User.class));
        likeRecordService.multiSetIsLikedByViewer(l2List, LikeRecord.TYPE_CML2, operator.getUserId());
        return l2List;
    }

    public Result<CommentL2> readCommentWithAllFalse(Integer commentId, Operator operator) {
        return readComment(commentId, false, false, false, false, operator);
    }

    public Result<CommentL2> readCommentWithContent(Integer commentId, Operator operator) {
        return readComment(commentId, false, false, true, false, operator);
    }

    public Result<CommentL2> readCommentWithCounter(Integer commentId, Operator operator) {
        return readComment(commentId, false, true, false, false, operator);
    }

    /**
     * 读取评论
     */
    public Result<CommentL2> readComment(Integer commentId, boolean withCreator, boolean withCounter, boolean withContent, boolean withIsLikedByViewer, Operator operator) {
        return serviceHelper.readComment(commentId, new CommentL2(), withCreator, withCounter, withContent, withIsLikedByViewer, operator);
    }

    /**
     * 读取指定一级评论的二级评论
     */
    public Result<List<CommentL2>> readIndexCommentL2List(@NotNull CommentL2 comment, Operator operator) {
        if (!comment.isValidReadIndexParam(previewCml2Num, true)) {
            return new Result<List<CommentL2>>(FAIL).setErrorCode(1010090203).setMessage("操作错误，参数不合法");
        }
        Result<Object> validateTokenResult = QueryTokenUtil.validateTokenAndSetQueryParam(comment, POPULAR, hmacKey);
        if (validateTokenResult.getState() == FAIL) {
            return new Result<List<CommentL2>>(validateTokenResult);
        }
        //在serviceHelper.readIndexComment中需要判断l1的l2_cnt，所以需要带counter
        Result<CommentL1> commentL1Result = commentL1Service.readCommentWithCounter(comment.getL1Id(), operator);
        if (commentL1Result.getState() == FAIL) {
            return new Result<List<CommentL2>>(commentL1Result);
        }
        Result<Post> postResult = postService.readPostWithAllFalse(commentL1Result.getObject().getPostId(), operator);
        if (postResult.getState() == FAIL) {
            return new Result<List<CommentL2>>(postResult);
        }
        comment.setL1(commentL1Result.getObject().setPost(postResult.getObject()));
        long l2Ofl1Cnt = commentL1Result.getObject().getL2Cnt();
        if (comment.getQueryParam().getFrom().equals(0)
                && "skipPreview".equalsIgnoreCase(comment.getQueryParam().getCustomAddition())) {
            comment.getQueryParam().setFrom((int) (l2Ofl1Cnt < previewCml2Num ? l2Ofl1Cnt : previewCml2Num));
        }
        CommentL2 queryEntity = (CommentL2) new CommentL2().setQueryParam(new QueryParam());
        Result<List<CommentL2>> result = serviceHelper.readIndexComment(comment, queryEntity, POPULAR, operator);
        if (result.getState() == FAIL) {
            return result;
        }
        multiSetReplyToUser(result.getObject());
        commentL2Validator.multiSetRights(comment, result.getObject(), operator);
        String newToken = QueryTokenUtil.generateQueryToken(comment, result.getObject(), queryEntity.getQueryParam(), POPULAR, hmacKey);
        return result.setAttachedToken(newToken);
    }

    @SneakyThrows
    private Result<Object> multiSetReplyToUser(@NotNull List<CommentL2> commentList) {
        return userService.multiSetUser(commentList, CommentL2.class.getMethod("getReplyToUserId"), CommentL2.class.getMethod("setReplyToUser", User.class));
    }

    /**
     * 发表评论
     */
    public Result<Integer> createComment(@NotNull CommentL2 commentL2, Operator operator) {
        if (!commentL2.isValidInsertParam()) {
            return new Result<Integer>(FAIL).setErrorCode(1010090204).setMessage("操作错误，参数不合法");
        }
        if (!commentL2Validator.checkCreatePermission(commentL2, operator)) {
            return new Result<Integer>(FAIL).setErrorCode(1010090205).setMessage("操作失败，权限错误");
        }
        Result<CommentL1> commentL1Result = commentL1Service.readCommentWithCounter(commentL2.getL1Id(), operator);
        if (commentL1Result.getState() == FAIL) {
            return new Result<Integer>(commentL1Result);
        }
        CommentL1 commentL1 = commentL1Result.getObject();
        Result<Post> postResult = postService.readPostWithAllFalse(commentL1.getPostId(), operator);
        // 帖子不存在
        if (postResult.getState() == FAIL) {
            return new Result<Integer>(postResult);
        }
        // 其他二级评论的回复
        if (commentL2.getReplyToL2Id() != null) {
            Result<CommentL2> replyToTargetResult = readCommentWithAllFalse(commentL2.getReplyToL2Id(), operator);
            if (replyToTargetResult.getState() == FAIL) {
                return new Result<Integer>(FAIL).setErrorCode(1010090210).setMessage("回复对象不存在");
            }
            CommentL2 replyToTarget = replyToTargetResult.getObject();
            if (!commentL2.getL1Id().equals(replyToTarget.getL1Id())) {
                return new Result<Integer>(FAIL).setErrorCode(1010090211).setMessage("回复对象不在同一评论下");
            }
            commentL2.setReplyToUserId(replyToTarget.getCreatorId());
        }
        commentL2.setPostId(commentL1.getPostId()).setCreator(operator.getUser());
        if (commentL1.getL2Cnt() < previewCml2Num) {
            commentL2.setPIndexedFlag(true);
        }
        int res = insertOne(commentL2);
        if (res == -1) {
            return new Result<Integer>(FAIL).setErrorCode(1010090106).setMessage("数据库操作失败");
        }
        commentL2.setLikeCnt(0L);
        if (commentL1.getL2Cnt() < previewCml2Num) {
            serviceHelper.addNewToRedisIndex(commentL2, POPULAR);
        }
        Long pcmCnt = null;
        // 发帖人回复
        if (operator.getUserId().equals(postResult.getObject().getCreatorId())) {
            pcmCnt = 1L;
        }
        commentL1Service.updateCounters(commentL1.getId(), 1L, pcmCnt);
        // 如果是回复的其他二级评论，则给被回复的二级评论创建者推送二级评论的回复通知
        // 否则给对应的一级评论创建者推送回复通知
        if (commentL2.getReplyToL2Id() != null && !commentL2.getReplyToUserId().equals(operator.getUserId())) {
            noticeService.pushNotice(commentL2.getReplyToL2Id(), Notice.TYPE_REPLY_OF_CML2, commentL2.getReplyToUserId());
        } else if (!commentL1.getCreatorId().equals(operator.getUserId())) {
            noticeService.pushNotice(commentL1.getId(), Notice.TYPE_CML2_OF_CML1, commentL1.getCreatorId());
        }
        return new Result<Integer>(SUCCESS).setObject(commentL2.getId());
    }

    public String correctCounters() {
        int r = dao.correctLikeCnt();
        if (r == 0) {
            return "二级评论数据校正错误，请重新校正";
        }
        return "二级评论的点赞数校正" + r + "条数据";
    }

    /**
     * 删除评论
     */
    @SneakyThrows
    public Result<Object> deleteComment(@NotNull CommentL2 param, Operator operator) {
        if (!param.isValidDeleteParam()) {
            return new Result<Object>(FAIL).setErrorCode(1010090207).setMessage("操作错误，参数不合法");
        }
        Result<CommentL2> commentL2Result = readCommentWithContent(param.getId(), operator);
        if (commentL2Result.getState() == FAIL) {
            return new Result<Object>(commentL2Result);
        }
        CommentL2 commentL2 = commentL2Result.getObject();
        Result<CommentL1> commentL1Result = commentL1Service.readCommentWithAllFalse(commentL2.getL1Id(), operator);
        if (commentL1Result.getState() == FAIL) {
            return new Result<Object>(commentL1Result);
        }
        CommentL1 commentL1 = commentL1Result.getObject();
        Result<Post> postResult = postService.readPostWithAllFalse(commentL1.getPostId(), operator);
        if (postResult.getState() == FAIL) {
            return new Result<Object>(postResult);
        }
        commentL2.setL1(commentL1.setPost(postResult.getObject()));
        if (!commentL2Validator.checkDeletePermission(commentL2, operator)) {
            return new Result<Object>(FAIL).setErrorCode(1010090208).setMessage("操作失败，权限错误");
        }
        boolean isAdminOp = !commentL2.getCreatorId().equals(operator.getUserId());
        if (isAdminOp && !param.isValidOpComment()) {
            return new Result<Object>(FAIL).setErrorCode(1010090212).setMessage("操作错误，操作原因字数在5-256");
        }
        int res = deleteOne(commentL2);
        if (res == 0) {
            return new Result<Object>(FAIL).setErrorCode(1010090209).setMessage("数据库操作失败");
        }
        Long pcmCnt = null;
        // 发帖人删除回复
        if (operator.getUserId().equals(postResult.getObject().getCreatorId()))
        {
            pcmCnt = -1L;
        }
        serviceHelper.removeRedisComment(commentL2.getL1Id(), commentL2.getId());
        commentL1Service.updateCounters(commentL1.getId(), -1L, pcmCnt);
        if (isAdminOp) {
            AdminOpsRecord record = new AdminOpsRecord();
            record.setTargetId(commentL2.getCreatorId()).setTarget(commentL2).setDesc(fullObjectMapper.writeValueAsString(commentL2)).setOpComment(param.getOpComment()).setType(AdminOpsRecord.TYPE_DELETE_CML2);
            adminOpsService.createAdminOpsRecord(record, operator);
        }
        return new Result<Object>(Result.SUCCESS);
    }

    /**
     * 每天清空一级评论的二级评论索引
     */
    @Scheduled(cron = "0 20 3 * * ?")
    public void dailyFlushRedisToDb() {
        Set<String> zSetKeys = RedisUtil.scanAllKeys(redisTemplate, serviceHelper.getRedisIndexZSetKey("*", POPULAR));
        @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming") List<TmpEntry> topNL2IdStrList = new ArrayList<>(zSetKeys.size());
        int end = previewCml2Num - 1;
        for (String key : zSetKeys) {
            Integer l1Id = Integer.valueOf(key.split(":")[1]);
            Set<String> topL2IdStr = redisTemplate.opsForZSet().reverseRange(key, 0, end);
            String str = "";
            for (String l2IdStr : topL2IdStr) {
                str += l2IdStr + ";";
            }
            topNL2IdStrList.add(new TmpEntry(l1Id, str));
        }
        commonService.clearIndexedFlag("lpt_comment_l2", "comment_l2_id", "comment_l2_p_indexed_flag", null);
        redisTemplate.delete(zSetKeys);
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getBasicKey("*")));
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getContentKey("*")));
        log.info("一级评论的二级评论索引清除");
        commonService.batchUpdate("lpt_comment_l1", "comment_l1_id", "comment_l1_top_n_l2_id_str", CommonService.OPS_COPY, topNL2IdStrList);
        log.info("一级评论的预览二级评论索引写入数据库");
        // 更新hmac的key
        hmacKey = CryptUtil.randUrlSafeStr(64, true);
    }

}
