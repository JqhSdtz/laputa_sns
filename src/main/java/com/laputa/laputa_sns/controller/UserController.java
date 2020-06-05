package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.Category;
import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.model.User;
import com.laputa.laputa_sns.model.UserRecvSetting;
import com.laputa.laputa_sns.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author JQH
 * @since 下午 4:01 20/02/13
 */

@Slf4j
@RestController
@RequestMapping(value = "/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "/check_name/{nickName}", method = RequestMethod.GET)
    public Result checkNickName(@PathVariable String nickName, @RequestAttribute Operator operator) {
        return userService.checkNickName(nickName).setOperator(operator);
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    public Result<User> readUser(@PathVariable  Integer userId, @RequestAttribute Operator operator) {
        return userService.readUser(userId, false, true, true, operator).setOperator(operator);
    }

    @RequestMapping(value = "/info", method = RequestMethod.PATCH)
    public Result updateUserInfo(@RequestBody User user, @RequestAttribute Operator operator) {
        return userService.updateUserInfo(user, operator).setOperator(operator);
    }

    @RequestMapping(value = "/name", method = RequestMethod.PATCH)
    public Result updateUserName(@RequestBody User user, @RequestAttribute Operator operator) {
        return userService.updateUserName(user, operator).setOperator(operator);
    }

    @RequestMapping(value = "/recv_setting", method = RequestMethod.PATCH)
    public Result updateRecvSetting(@RequestBody UserRecvSetting recvSetting, @RequestAttribute Operator operator) {
        return userService.updateRecvSetting(recvSetting, operator).setOperator(operator);
    }

    @RequestMapping(value = "/set_talk_ban", method = RequestMethod.PATCH)
    public Result setTalkBanTo(@RequestBody User user, @RequestAttribute Operator operator) {
        return userService.setTalkBanTo(user, operator).setOperator(operator);
    }

    @RequestMapping(value = "/top_post/{action}", method = RequestMethod.PATCH)
    public Result setUserTopPost(@RequestBody User user, @PathVariable String action, @RequestAttribute Operator operator) {
        if ("create".equals(action))
            return userService.setTopPost(user, false, operator).setOperator(operator);
        if ("cancel".equals(action))
            return userService.setTopPost(user, true, operator).setOperator(operator);
        return Result.EMPTY_FAIL;
    }

    @RequestMapping(value = "/recent_visit_categories", method = RequestMethod.GET)
    public Result<List<UserService.RecentVisitedCategory>> readRecentVisitCategories(@RequestAttribute Operator operator) {
        return userService.readRecentVisitedCategories(operator).setOperator(operator);
    }

    @RequestMapping(value = "/pin_category/{categoryId}", method = RequestMethod.POST)
    public Result pinRecentVisitCategory(@PathVariable Integer categoryId, @RequestAttribute Operator operator) {
        return userService.pinUserRecentVisitCategory(categoryId, false, operator).setOperator(operator);
    }

    @RequestMapping(value = "/unpin_category/{categoryId}", method = RequestMethod.POST)
    public Result unPinRecentVisitCategory(@PathVariable Integer categoryId, @RequestAttribute Operator operator) {
        return userService.pinUserRecentVisitCategory(categoryId, true, operator).setOperator(operator);
    }

}
