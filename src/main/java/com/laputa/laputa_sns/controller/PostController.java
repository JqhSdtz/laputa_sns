package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.annotation.AccessLimit;
import com.laputa.laputa_sns.annotation.LimitTimeUnit;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.service.PostIndexService;
import com.laputa.laputa_sns.service.PostService;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author JQH
 * @since 下午 3:32 20/02/23
 */

@RestController
@RequestMapping(value = "/api/post")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @AccessLimit(value = 20, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 5, per = LimitTimeUnit.MINUTE)
    @RequestMapping(value = "/public", method = RequestMethod.POST)
    public Result<Integer> createPublicPost(@NotNull @RequestBody Post post, @RequestAttribute Operator operator) {
        return postService.createPost((Post) post.setType(Post.TYPE_PUBLIC), operator).setOperator(operator);
    }

    @AccessLimit(value = 40, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 10, per = LimitTimeUnit.MINUTE)
    @RequestMapping(value = "/private", method = RequestMethod.POST)
    public Result<Integer> createPrivatePost(@NotNull @RequestBody Post post, @RequestAttribute Operator operator) {
        return postService.createPost((Post) post.setType(Post.TYPE_PRIVATE), operator).setOperator(operator);
    }

    @RequestMapping(value = "/{postId}", method = RequestMethod.GET)
    public Result<Post> readPost(@PathVariable Integer postId, @RequestAttribute Operator operator) {
        return postService.readPost(postId, true, true, true, true, true, true, 5, true, operator).setOperator(operator);
    }

    @RequestMapping(value = "/full_text/{fullTextId}", method = RequestMethod.GET)
    public Result<String> readFullText(@PathVariable Integer fullTextId, @RequestAttribute Operator operator) {
        return postService.readFullText(fullTextId).setOperator(operator);
    }

    @RequestMapping(value = "/popular", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<Post>> readPopularList(@RequestBody Post post, @RequestAttribute Operator operator) {
        return postService.readIndexPostList(post, PostIndexService.POPULAR, operator).setOperator(operator);
    }

    @RequestMapping(value = "/latest", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<Post>> readLatestList(@RequestBody Post post, @RequestAttribute Operator operator) {
        return postService.readIndexPostList(post, PostIndexService.LATEST, operator).setOperator(operator);
    }

    @RequestMapping(value = "/creator", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<Post>> readListOfCreator(@RequestBody Post post, @RequestAttribute Operator operator) {
        return postService.readPostListOfCreator(post, operator).setOperator(operator);
    }

    @RequestMapping(value = "/top_comment/{action}", method = RequestMethod.PATCH)
    public Result<Object> setTopComment(@RequestBody Post post, @PathVariable String action, @RequestAttribute Operator operator) {
        if ("create".equals(action)) {
            return postService.setTopComment(post, false, operator).setOperator(operator);
        }
        if ("cancel".equals(action)) {
            return postService.setTopComment(post, true, operator).setOperator(operator);
        }
        return new Result<Object>(Result.FAIL);
    }

    @AccessLimit(value = 3, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 1, per = LimitTimeUnit.MINUTE)
    @RequestMapping(value = "/category", method = RequestMethod.PATCH)
    public Result<Object> setCategory(@RequestBody Post post, @RequestAttribute Operator operator) {
        return postService.setCategory(post, operator).setOperator(operator);
    }

    @AccessLimit(value = 40, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 10, per = LimitTimeUnit.MINUTE)
    @RequestMapping(value = "/content", method = RequestMethod.PATCH)
    public Result<Object> updateContent(@RequestBody Post post, @RequestAttribute Operator operator) {
        return postService.updateContent(post, operator).setOperator(operator);
    }

    @AccessLimit(value = 40, per = LimitTimeUnit.HOUR)
    @AccessLimit(value = 10, per = LimitTimeUnit.MINUTE)
    @RequestMapping(method = RequestMethod.DELETE)
    public Result<Object> deletePost(@RequestBody Post post, @RequestAttribute Operator operator) {
        return postService.deletePost(post, operator).setOperator(operator);
    }

//    @RequestMapping(value = "/test", method = RequestMethod.GET)
//    public List<Post> test() {
//        return postService.test();
//    }

}
