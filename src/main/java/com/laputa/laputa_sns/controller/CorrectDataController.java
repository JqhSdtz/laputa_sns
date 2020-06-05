package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.service.*;
import com.laputa.laputa_sns.validator.AdminLevel;
import org.springframework.web.bind.annotation.*;

/**
 * @author JQH
 * @since 下午 12:56 20/04/26
 */

@RestController
@RequestMapping(value = "/correct_data")
public class CorrectDataController {
//$.post(baseUrl + '/correct_data/all',function(data){console.log(data)})
    private final UserService userService;
    private final CategoryService categoryService;
    private final PostService postService;
    private final CommentL1Service commentL1Service;
    private final CommentL2Service commentL2Service;

    public CorrectDataController(UserService userService, CategoryService categoryService, PostService postService, CommentL1Service commentL1Service, CommentL2Service commentL2Service) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.postService = postService;
        this.commentL1Service = commentL1Service;
        this.commentL2Service = commentL2Service;
    }

    @RequestMapping(value = "/{type}", method = RequestMethod.POST)
    public String correctUserCnt(@PathVariable String type, @RequestAttribute Operator operator) {
        if (!operator.isSuperAdmin())
            return "无权限";
        if ("user".equals(type))
            return userService.correctCounters();
        if ("category".equals(type))
            return categoryService.correctCounters();
        if ("post".equals(type))
            return postService.correctCounters();
        if ("cml1".equals(type))
            return commentL1Service.correctCounters();
        if ("cml2".equals(type))
            return commentL2Service.correctCounters();
        if ("all".equals(type))
            return userService.correctCounters() + "\n" + categoryService.correctCounters() + "\n" + postService.correctCounters() + "\n" + commentL1Service.correctCounters() + "\n" + commentL2Service.correctCounters();
        return "参数错误";
    }
}
