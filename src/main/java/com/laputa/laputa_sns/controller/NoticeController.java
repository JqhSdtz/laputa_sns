package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.QueryParam;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.Notice;
import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.service.NoticeService;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author JQH
 * @since 下午 1:46 20/04/20
 */

@RestController
@RequestMapping(value = "/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<Notice>> readNoticeList(@RequestBody QueryParam queryParam, @RequestAttribute Operator operator) {
        return noticeService.pullNotice(queryParam, operator).setOperator(operator);
    }

    @RequestMapping(value = "/read", method = {RequestMethod.POST})
    public Result markNoticeAsRead(@RequestBody Notice notice, @RequestAttribute Operator operator) {
        return noticeService.markNoticeAsRead(notice, operator).setOperator(operator);
    }
}
