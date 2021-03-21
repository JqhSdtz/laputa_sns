package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.User;
import com.laputa.laputa_sns.service.OperatorService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author JQH
 * @since 下午 12:31 20/02/19
 */

@RestController
@RequestMapping(value = "/api/operator")
public class OperatorController {

    private final OperatorService operatorService;

    public OperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Result<Operator> login(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        return operatorService.login(user, request, response);
    }

    @RequestMapping(value = "/register/{extra64}/{rand}/{calRes}", method = RequestMethod.POST)
    public Result<Operator> register(@RequestBody User user, @PathVariable String extra64,
                                     @PathVariable String rand, @PathVariable String calRes,
                                     HttpServletRequest request, HttpServletResponse response) {
        return operatorService.registerWithPow(user, extra64, rand, calRes, request, response);
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public Result logout(@RequestAttribute Operator operator, HttpServletResponse response) {
        return operatorService.logout(operator, response).setOperator(new Operator(-1));
    }

    @RequestMapping(value = "/empty")
    public Result emptyAction(@RequestAttribute Operator operator) {
        return new Result(Result.SUCCESS).setOperator(operator);
    }

}
