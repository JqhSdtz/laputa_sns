package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.service.ForwardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author JQH
 * @since 上午 11:14 20/03/09
 */

@RestController
@RequestMapping(value = "/api/forward")
public class ForwardController {
    private final ForwardService forwardService;

    public ForwardController(ForwardService forwardService) {
        this.forwardService = forwardService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result<Integer> createForward(@RequestBody Post post, @RequestAttribute Operator operator) {
        return forwardService.createForward(post, operator).setOperator(operator);
    }

    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<Post>> readForwardList(@RequestBody Post post, @RequestAttribute Operator operator) {
        return forwardService.readForwardList(post, operator).setOperator(operator);
    }
}
