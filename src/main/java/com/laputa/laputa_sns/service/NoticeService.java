package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.QueryParam;
import com.laputa.laputa_sns.common.RedisPrefix;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author JQH
 * @since 下午 2:38 20/04/15
 */

@Service
@EnableScheduling
public class NoticeService {

    private final PostService postService;
    private final CommentL1Service commentL1Service;
    private final CommentL2Service commentL2Service;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> pushNoticeScript;
    private final DefaultRedisScript<List> pullNoticeScript;

    public NoticeService(@Lazy PostService postService, @Lazy CommentL1Service commentL1Service, @Lazy CommentL2Service commentL2Service, StringRedisTemplate redisTemplate) {
        this.postService = postService;
        this.commentL1Service = commentL1Service;
        this.commentL2Service = commentL2Service;
        this.redisTemplate = redisTemplate;
        this.pushNoticeScript = new DefaultRedisScript(
                "redis.call('zadd', KEYS[1], ARGV[2], ARGV[1])\n" + "local initTime = redis.call('hget', KEYS[2], ARGV[1])\n" + "if (initTime == nil or initTime == false or initTime == '0') then\n" +
                        "\tredis.call('hset', KEYS[2], ARGV[1], ARGV[2])\n" + "\tredis.call('hset', KEYS[3], ARGV[1], ARGV[3])\n" + "else\n" + "\tredis.call('hincrby', KEYS[3], ARGV[1], ARGV[3])\n" + "end\n" +
                        "local len = redis.call('zcard', KEYS[1])\n" + "if (len > tonumber(ARGV[4])) then\n" + "\tlocal lstKey = redis.call('zrange', KEYS[1], 0, 0)[1]\n" +
                        "\tredis.call('zremrangebyrank', KEYS[1], 0, 0)\n" + "\tredis.call('hdel', KEYS[2], lstKey)\n" +
                        "\tredis.call('hdel', KEYS[3], lstKey)\n" + "end\n" + "return len", Long.class);
        this.pullNoticeScript = new DefaultRedisScript(
                "local zList = redis.call('zrevrange', KEYS[1], tonumber(ARGV[1]), tonumber(ARGV[2]), 'WITHSCORES')\n" + "local len = #zList / 2\n" +
                        "if (len > 0) then\n" + "\tlocal fileds = {}\n" + "\tfor i = 1, len do\n" + "\t\ttable.insert(fileds, zList[i * 2 - 1])\n" + "\tend\n" +
                        "\tlocal hList1 = redis.call('hmget', KEYS[2], unpack(fileds))\n" + "\tlocal hList2 = redis.call('hmget', KEYS[3], unpack(fileds))\n" + "\tfor i = 1, #hList1 do table.insert(zList, hList1[i]) end\n" +
                        "\tfor i = 1, #hList2 do table.insert(zList, hList2[i]) end\n" + "end\n" + "return zList", List.class);
    }

    @Value("${user-notice-box-length}")//用户消息接收箱最大长度
    private int userNoticeBoxLength;

    @NotNull
    private String getRedisTimeKey(Integer receiverId) {
        return RedisPrefix.USER_NOTICE_BOX_TIME + ":" + receiverId;
    }

    @NotNull
    private String getRedisInitTimeKey(Integer receiverId) {
        return RedisPrefix.USER_NOTICE_BOX_INIT_TIME + ":" + receiverId;
    }

    @NotNull
    private String getRedisCntKey(Integer receiverId) {
        return RedisPrefix.USER_NOTICE_BOX_CNT + ":" + receiverId;
    }

    private long executePushScript(String zKey, String hKey1, String hKey2, String item, long delta, long timeStamp, int limit) {
        return redisTemplate.execute(pushNoticeScript, Arrays.asList(zKey, hKey1, hKey2), item, String.valueOf(timeStamp), String.valueOf(delta), String.valueOf(limit));
    }

    public long pushNotice(int contentId, int type, int receiverId) {
        return executePushScript(getRedisTimeKey(receiverId), getRedisInitTimeKey(receiverId), getRedisCntKey(receiverId), type + ":" + contentId, 1, new Date().getTime(), userNoticeBoxLength);
    }

    public long pullNoticeCnt(int receiverId) {
        List<Object> resList = redisTemplate.executePipelined((RedisCallback) connection -> {
            connection.hGetAll(getRedisInitTimeKey(receiverId).getBytes());
            connection.hGetAll(getRedisCntKey(receiverId).getBytes());
            return null;
        });
        Map<String, String> initTimeMap = (Map<String, String>) resList.get(0);
        Map<String, String> cntMap = (Map<String, String>) resList.get(1);
        long cnt = 0;
        for (Map.Entry<String, String> entry : initTimeMap.entrySet()) {
            if ("0".equals(entry.getValue()))
                continue;
            cnt += Long.valueOf(cntMap.get(entry.getKey()));
        }
        return cnt;
    }

    public Result<List<Notice>> pullNotice(@NotNull QueryParam queryParam, @NotNull Operator operator) {
        if (queryParam.getFrom() == null || queryParam.getQueryNum() == null || queryParam.getQueryNum() > 10)
            return new Result(Result.FAIL).setErrorCode(1010160201).setMessage("操作错误，参数不合法");
        int receiverId = operator.getUserId();
        List<String> resList = redisTemplate.execute(pullNoticeScript, Arrays.asList(getRedisTimeKey(receiverId), getRedisInitTimeKey(receiverId), getRedisCntKey(receiverId)),
                String.valueOf(queryParam.getFrom()), String.valueOf(queryParam.getFrom() + queryParam.getQueryNum() - 1));
        int len = resList.size() / 4;
        List<Notice> noticeList = new ArrayList(len);
        int initTimeStart = len * 2;
        int cntStart = len * 3;
        for (int i = 0; i < len; ++i) {
            String[] item = resList.get(i * 2).split(":");
            Notice notice = new Notice().setType(Integer.valueOf(item[0])).setContentId(Integer.valueOf(item[1]))
                    .setUpdateTime(new Date(Long.valueOf(resList.get(i * 2 + 1)))).setUnreadCnt(Long.valueOf(resList.get(cntStart + i)))
                    .setInitTime(new Date(Long.valueOf(resList.get(initTimeStart + i))));
            noticeList.add(notice);
        }
        fillNoticeWithContent(noticeList, operator);
        return new Result(Result.SUCCESS).setObject(noticeList);
    }

    public Result markNoticeAsRead(@NotNull Notice notice, @NotNull Operator operator) {
        if (notice.getContentId() == null || notice.getType() == null)
            return new Result(Result.FAIL).setErrorCode(1010160202).setMessage("操作失败，参数错误");
        redisTemplate.opsForHash().put(getRedisInitTimeKey(operator.getUserId()), notice.getType() + ":" + notice.getContentId(), "0");
        return Result.EMPTY_SUCCESS;
    }

    private void fillNoticeWithContent(@NotNull List<Notice> noticeList, Operator operator) {
        Map<Integer, Post> postMap = new HashMap();
        Map<Integer, CommentL1> cml1Map = new HashMap();
        Map<Integer, CommentL2> cml2Map = new HashMap();
        for (int i = 0; i < noticeList.size(); ++i) {
            Notice notice = noticeList.get(i);
            int type = notice.getType(), id = notice.getContentId();
            if (type == Notice.TYPE_LIKE_POST || type == Notice.TYPE_CML1_OF_POST || type == Notice.TYPE_FW_POST)
                postMap.put(id, null);
            else if (type == Notice.TYPE_LIKE_CML1 || type == Notice.TYPE_CML2_OF_CML1)
                cml1Map.put(id, null);
            else if (type == Notice.TYPE_LIKE_CML2)
                cml2Map.put(id, null);
        }
        if (postMap.size() != 0) {
            List<Post> postList = (List<Post>) postService.multiReadPostWithContentAndCounter(new ArrayList(postMap.keySet()), operator).getObject();
            if (postList != null)
                for (int i = 0; i < postList.size(); ++i)
                    postMap.put(postList.get(i).getId(), postList.get(i));
        }
        if (cml1Map.size() != 0) {
            List<CommentL1> cml1List = (List<CommentL1>) commentL1Service.multiReadCommentWithContentAndCounter(new ArrayList(cml1Map.keySet())).getObject();
            if (cml1List != null)
                for (int i = 0; i < cml1List.size(); ++i)
                    cml1Map.put(cml1List.get(i).getId(), cml1List.get(i));
        }
        if (cml2Map.size() != 0) {
            List<CommentL2> cml2List = (List<CommentL2>) commentL2Service.multiReadCommentWithContentAndCounter(new ArrayList(cml2Map.keySet())).getObject();
            if (cml2List != null)
                for (int i = 0; i < cml2List.size(); ++i)
                    cml2Map.put(cml2List.get(i).getId(), cml2List.get(i));
        }
        for (int i = 0; i < noticeList.size(); ++i) {
            Notice notice = noticeList.get(i);
            int type = notice.getType(), id = notice.getContentId();
            if (type == Notice.TYPE_LIKE_POST || type == Notice.TYPE_CML1_OF_POST || type == Notice.TYPE_FW_POST)
                notice.setContent(postMap.get(id));
            else if (type == Notice.TYPE_LIKE_CML1 || type == Notice.TYPE_CML2_OF_CML1)
                notice.setContent(cml1Map.get(id));
            else if (type == Notice.TYPE_LIKE_CML2)
                notice.setContent(cml2Map.get(id));
        }
    }
}
