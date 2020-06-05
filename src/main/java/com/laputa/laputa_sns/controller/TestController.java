package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author JQH
 * @since 下午 6:33 20/02/25
 */

@Slf4j
@RestController
@RequestMapping(value = "/test")
public class TestController {

    private final LikeRecordService likeRecordService;
    private final PostService postService;
    private final CommentL1Service commentL1Service;
    private final CommentL2Service commentL2Service;
    private final ForwardService forwardService;
    private final UserService userService;
    private final FollowService followService;
    private final PostNewsService postNewsService;
    private final PostIndexService postIndexService;
    private final CategoryService categoryService;

    public TestController(LikeRecordService likeRecordService, PostService postService, CommentL1Service commentL1Service, CommentL2Service commentL2Service, ForwardService forwardService, UserService userService, CategoryService categoryService, FollowService followService, PostNewsService postNewsService, PostIndexService postIndexService, CategoryService categoryService1) {
        this.likeRecordService = likeRecordService;
        this.postService = postService;
        this.commentL1Service = commentL1Service;
        this.commentL2Service = commentL2Service;
        this.forwardService = forwardService;
        this.userService = userService;
        this.followService = followService;
        this.postNewsService = postNewsService;
        this.postIndexService = postIndexService;
        this.categoryService = categoryService1;
    }

    @RequestMapping(value = "/flush")
    public void write() {
        //categoryService.dailyFlushPostCntToDB();
        likeRecordService.dailyFlushRedisLikeRecordToDB();
        postService.dailyFlushRedisToDB();
        commentL1Service.dailyFlushRedisToDB();
        commentL2Service.dailyFlushRedisToDB();
        forwardService.dailyFlushRedisForwardRecordToDB();
        userService.dailyFlushRedisToDB();
        followService.dailyFlushRedisToDB();
        postNewsService.dailyFreshIndex();
        postIndexService.dailyFlushPostIndex();
        categoryService.dailyFlushRedisToDB();
    }

}
