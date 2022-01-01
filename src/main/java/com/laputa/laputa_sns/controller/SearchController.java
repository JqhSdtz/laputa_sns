package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.SearchService;
import org.springframework.web.bind.annotation.*;

/**
 * @author JQH
 * @since 下午 1:12 20/05/14
 */

@RestController
@RequestMapping(value = "/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @RequestMapping(value = "/{type}/{words}/{mode}", method = RequestMethod.GET)
    public Result<?> search(@PathVariable("type") String type, @PathVariable("words") String words, @PathVariable("mode") String mode, @RequestAttribute Operator operator) {
        if ("post".equals(type)) {
            return searchService.searchPost(words, mode, operator);
        } else if ("category".equals(type)) {
            return searchService.searchCategory(words, mode, operator);
        } else if ("user".equals(type)) {
            return searchService.searchUser(words, mode, operator);
        } else {
            return new Result<Object>(Result.FAIL);
        }
    }
}
