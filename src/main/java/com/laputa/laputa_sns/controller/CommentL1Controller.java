package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.annotation.AccessLimit;
import com.laputa.laputa_sns.annotation.AccessLimitTarget;
import com.laputa.laputa_sns.annotation.LimitTimeUnit;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.helper.CommentServiceHelper;
import com.laputa.laputa_sns.model.entity.CommentL1;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.CommentL1Service;
import org.springframework.web.bind.annotation.*;

/**
 * @author JQH
 * @since 下午 4:17 20/02/29
 */

@RestController
@RequestMapping(value = "/api/comment/l1")
public class CommentL1Controller {

    private final CommentL1Service commentL1Service;

    public CommentL1Controller(CommentL1Service commentL1Service) {
        this.commentL1Service = commentL1Service;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Result<CommentL1> readComment(@PathVariable Integer id, @RequestAttribute Operator operator) {
        return commentL1Service.readComment(id, true, true, true, true, operator).setOperator(operator);
    }

    @AccessLimit(value = 40, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 10, per = LimitTimeUnit.MINUTE)
    @RequestMapping(method = RequestMethod.POST)
    public Result createComment(@AccessLimitTarget(byMethod = "getPost") @RequestBody CommentL1 comment, @RequestAttribute Operator operator) {
        return commentL1Service.createComment(comment, operator).setOperator(operator);
    }

    @AccessLimit(value = 40, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 10, per = LimitTimeUnit.MINUTE)
    @RequestMapping(method = RequestMethod.DELETE)
    public Result deleteComment(@AccessLimitTarget(byMethod = "getPost") @RequestBody CommentL1 comment, @RequestAttribute Operator operator) {
        return commentL1Service.deleteComment(comment, operator).setOperator(operator);
    }

    @RequestMapping(value = "/popular", method = {RequestMethod.GET, RequestMethod.POST})
    public Result readPopularCommentL1List(@RequestBody CommentL1 comment, @RequestAttribute Operator operator) {
        return commentL1Service.readIndexCommentL1List(comment, CommentServiceHelper.POPULAR, true, operator).setOperator(operator);
    }

    @RequestMapping(value = "/latest", method = {RequestMethod.GET, RequestMethod.POST})
    //还在考虑，现在是按时间读取评论列表直接读数据库，考虑取消这个功能或者缓存中再加一个按时间排序的列表
    public Result readLatestCommentL1List(@RequestBody CommentL1 comment, @RequestAttribute Operator operator) {
        return commentL1Service.readIndexCommentL1List(comment, CommentServiceHelper.LATEST, true, operator).setOperator(operator);
    }

}
