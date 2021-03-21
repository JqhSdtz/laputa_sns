package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Follow;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author JQH
 * @since 下午 9:41 20/02/20
 */

@Slf4j
@RestController
@RequestMapping(value = "/api/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result<Integer> createFollow(@RequestBody Follow follow, @RequestAttribute Operator operator) {
        return followService.createFollow(follow, operator).setOperator(operator);
    }

    @RequestMapping(method = RequestMethod.PATCH)
    public Result updateFollow(@RequestBody Follow follow, @RequestAttribute Operator operator) {
        return followService.updateFollow(follow, operator).setOperator(operator);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result deleteFollow(@RequestBody Follow follow, @RequestAttribute Operator operator) {
        return followService.deleteFollow(follow, operator).setOperator(operator);
    }

    @RequestMapping(value = "/following/{userId}", method = RequestMethod.GET)
    public Result<List<Follow>> readFollowingList(@PathVariable Integer userId, @RequestAttribute Operator operator) {
        return followService.readFollowingList(userId,true, operator).setOperator(operator);
    }

    @RequestMapping(value = "/follower", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<Follow>> readFollowerList(@RequestBody Follow follow, @RequestAttribute Operator operator) {
        return followService.readFollowerList(follow, operator).setOperator(operator);
    }
}
