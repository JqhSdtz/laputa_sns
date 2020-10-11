package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.PostNews;
import com.laputa.laputa_sns.service.PostNewsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author JQH
 * @since 下午 3:25 20/03/10
 */

@RestController
@RequestMapping(value = "/news")
public class PostNewsController {

    private final PostNewsService postNewsService;

    public PostNewsController(PostNewsService postNewsService) {
        this.postNewsService = postNewsService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<PostNews>> readNewsList(@RequestBody PostNews postNews, @RequestAttribute Operator operator) {
        return postNewsService.readNewsList(postNews, operator).setOperator(operator);
    }

}
