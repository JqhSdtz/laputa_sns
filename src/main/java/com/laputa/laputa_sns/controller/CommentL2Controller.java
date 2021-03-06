package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.annotation.AccessLimit;
import com.laputa.laputa_sns.annotation.AccessLimitTarget;
import com.laputa.laputa_sns.annotation.LimitTimeUnit;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.CommentL2;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.CommentL2Service;
import org.springframework.web.bind.annotation.*;

/**
 * @author JQH
 * @since 下午 4:28 20/02/29
 */
@RestController
@RequestMapping(value = "/comment/l2")
public class CommentL2Controller {

    private final CommentL2Service commentL2Service;

    public CommentL2Controller(CommentL2Service CommentL2Service) {
        this.commentL2Service = CommentL2Service;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Result<CommentL2> readComment(@PathVariable Integer id, @RequestAttribute Operator operator) {
        return commentL2Service.readComment(id, true, true, true, true, operator).setOperator(operator);
    }

    @AccessLimit(value = 40, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 10, per = LimitTimeUnit.MINUTE)
    @RequestMapping(method = RequestMethod.POST)
    public Result createComment(@AccessLimitTarget(byMethod = "getL1") @RequestBody CommentL2 comment, @RequestAttribute Operator operator) {
        return commentL2Service.createComment(comment, operator).setOperator(operator);
    }

    @AccessLimit(value = 40, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 10, per = LimitTimeUnit.MINUTE)
    @RequestMapping(method = RequestMethod.DELETE)
    public Result deleteComment(@AccessLimitTarget(byMethod = "getL1") @RequestBody CommentL2 comment, @RequestAttribute Operator operator) {
        return commentL2Service.deleteComment(comment, operator).setOperator(operator);
    }

    @RequestMapping(value = "/popular", method = {RequestMethod.GET, RequestMethod.POST})
    public Result readPopularCommentL2List(@RequestBody CommentL2 comment, @RequestAttribute Operator operator) {
        return commentL2Service.readIndexCommentL2List(comment, operator).setOperator(operator);
    }

}

