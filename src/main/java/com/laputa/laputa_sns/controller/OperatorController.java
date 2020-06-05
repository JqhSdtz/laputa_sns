package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.model.User;
import com.laputa.laputa_sns.service.OperatorService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * @author JQH
 * @since 下午 12:31 20/02/19
 */

@RestController
@RequestMapping(value = "/operator")
public class OperatorController {

    private final OperatorService operatorService;

    public OperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Result<Operator> login(@RequestBody User user, HttpServletResponse response) {
        return operatorService.login(user, response);
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Result<Operator> register(@RequestBody User user, HttpServletResponse response) {
        return operatorService.register(user, response);
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public Result logout(@RequestAttribute Operator operator, HttpServletResponse response) {
        return operatorService.logout(operator, response).setOperator(new Operator(-1));
    }

}
