package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.service.CommentL1Service;
import com.laputa.laputa_sns.service.CommentL2Service;
import com.laputa.laputa_sns.service.PostService;
import com.laputa.laputa_sns.util.ProgOperatorManager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final CommentL1Service commentL1Service;
    private final CommentL2Service commentL2Service;
    private final Operator progOperator = ProgOperatorManager.register(SEOController.class);

    public SEOController(CategoryService categoryService, PostService postService, 
        CommentL1Service commentL1Service, CommentL2Service commentL2Service) {
        this.categoryService = categoryService;
        this.postService = postService;
        this.commentL1Service = commentL1Service;
        this.commentL2Service = commentL2Service;
    }

    private ModelAndView getModelAndView() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("categoryService", categoryService);
        modelAndView.addObject("postService", postService);
        modelAndView.addObject("operator", progOperator);
        modelAndView.addObject("commentL1Service", commentL1Service);
        modelAndView.addObject("commentL2Service", commentL2Service);
        return modelAndView;
    }

    @RequestMapping("/blog/post_detail/{postId}")
    public ModelAndView postDetail(@PathVariable Integer postId) {
        ModelAndView modelAndView = getModelAndView();
        modelAndView.addObject("postId", postId);
        modelAndView.setViewName("blog/post_detail.html");
        return modelAndView;
    }

    @RequestMapping("/blog/category_detail/{categoryId}")
    public ModelAndView categoryDetail(@PathVariable Integer categoryId) {
        ModelAndView modelAndView = getModelAndView();
        modelAndView.addObject("categoryId", categoryId);
        modelAndView.setViewName("blog/category_detail.html");
        return modelAndView;
    }

    @ResponseBody
    @RequestMapping(value = {"", "/", "/blog/index"})
    public ModelAndView index() {
        ModelAndView modelAndView = getModelAndView();
        modelAndView.setViewName("blog/index.html");
        return modelAndView;
    }
}
