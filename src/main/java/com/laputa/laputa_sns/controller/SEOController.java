package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.service.PostService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author JQH
 * @since 下午 8:16 21/04/19
 */

@Controller
public class SEOController {

    private final CategoryService categoryService;
    private final PostService postService;

    public SEOController(CategoryService categoryService, PostService postService) {
        this.categoryService = categoryService;
        this.postService = postService;
    }

    private ModelAndView getModelAndView(Operator operator) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("categoryService", categoryService);
        modelAndView.addObject("postService", postService);
        modelAndView.addObject("operator", operator);
        return modelAndView;
    }

    @RequestMapping("/blog/post_detail/{postId}")
    public ModelAndView postDetail(@PathVariable Integer postId, @RequestAttribute Operator operator) {
        ModelAndView modelAndView = getModelAndView(operator);
        modelAndView.addObject("postId", postId);
        modelAndView.setViewName("blog/post_detail.html");
        return modelAndView;
    }

    @RequestMapping("/blog/category_detail/{categoryId}")
    public ModelAndView categoryDetail(@PathVariable Integer categoryId, @RequestAttribute Operator operator) {
        ModelAndView modelAndView = getModelAndView(operator);
        modelAndView.addObject("categoryId", categoryId);
        modelAndView.setViewName("blog/category_detail.html");
        return modelAndView;
    }

    @ResponseBody
    @RequestMapping(value = {"", "/", "/blog/index"})
    public ModelAndView index(@RequestAttribute Operator operator) {
        ModelAndView modelAndView = getModelAndView(operator);
        modelAndView.setViewName("blog/index.html");
        return modelAndView;
    }
}
