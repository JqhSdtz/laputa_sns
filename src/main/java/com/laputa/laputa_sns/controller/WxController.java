package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.param.wx.CodeAndUserInfo;
import com.laputa.laputa_sns.service.WxService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author JQH
 * @since 下午 7:36 20/06/11
 */

@RestController
@RequestMapping(value = "/wx")
public class WxController {

    private final WxService wxService;

    public WxController(WxService wxService) {
        this.wxService = wxService;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Result login(@RequestBody CodeAndUserInfo codeAndUserInfo) {
        return wxService.login(codeAndUserInfo);
    }
}
