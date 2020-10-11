package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Category;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.view.CategoryView;
import com.laputa.laputa_sns.vo.CategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author JQH
 * @since 下午 8:35 20/02/06
 */

@Slf4j
@RestController
@RequestMapping(value = "/category")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryView categoryView;

    public CategoryController(CategoryService categoryService, CategoryView categoryView) {
        this.categoryService = categoryService;
        this.categoryView = categoryView;
    }

    @RequestMapping(value = "/{categoryId}", method = RequestMethod.GET)
    public Result<CategoryVo> readCategoryView(@PathVariable Integer categoryId, @RequestAttribute Operator operator) {
        return categoryView.readCategoryVo(categoryId, operator).setOperator(operator);
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result createCategory(@RequestBody Category category, @RequestAttribute Operator operator) {
        return categoryService.createCategory(category, operator).setOperator(operator);
    }

    @RequestMapping(value = "/info", method = RequestMethod.PATCH)
    public Result updateCategoryInfo(@RequestBody Category category, @RequestAttribute Operator operator) {
        return categoryService.updateCategoryInfo(category, operator).setOperator(operator);
    }

    @RequestMapping(value = "/disp_seq", method = RequestMethod.PATCH)
    public Result updateCategoryDispSeq(@RequestBody Category category, @RequestAttribute Operator operator) {
        return categoryService.updateCategoryDispSeq(category, operator).setOperator(operator);
    }

    @RequestMapping(value = "/cache_num", method = RequestMethod.PATCH)
    public Result updateCategoryCacheNum(@RequestBody Category category, @RequestAttribute Operator operator) {
        return categoryService.updateCategoryCacheNum(category, operator).setOperator(operator);
    }

    @RequestMapping(value = "/parent", method = RequestMethod.PATCH)
    public Result updateCategoryParent(@RequestBody Category category, @RequestAttribute Operator operator) {
        return categoryService.updateCategoryParent(category, operator).setOperator(operator);
    }

    @RequestMapping(value = "/top_post/{action}", method = RequestMethod.PATCH)
    public Result setCategoryTopPost(@RequestBody Category category, @PathVariable String action, @RequestAttribute Operator operator) {
        if ("create".equals(action))
            return categoryService.setCategoryTopPost(category, false, operator).setOperator(operator);
        if ("cancel".equals(action))
            return categoryService.setCategoryTopPost(category, true, operator).setOperator(operator);
        return Result.EMPTY_FAIL;
    }

    @RequestMapping(value = "/def_sub", method = RequestMethod.PATCH)
    public Result setDefaultSubCategory(@RequestBody Category category, @RequestAttribute Operator operator) {
        return categoryService.setDefaultSubCategory(category, false, operator).setOperator(operator);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result deleteCategory(@PathVariable Category category, @RequestAttribute Operator operator) {
        return categoryService.deleteCategory(category, operator, false).setOperator(operator);
    }

    @RequestMapping(value = "/roots", method = RequestMethod.GET)
    public Result<List<Category>> readRootCategoryList(@RequestAttribute Operator operator) {
        return categoryService.readRootCategoryList().setOperator(operator);
    }

    @RequestMapping(value = "/leaves", method = RequestMethod.GET)
    public Result<Set<Category>> readLeafCategorySet(@RequestAttribute Operator operator) {
        return categoryService.readLeafCategorySet().setOperator(operator);
    }

    @RequestMapping(value = "/leaves_of_root/{id}/{preventPrivate}", method = RequestMethod.GET)
    public Result<List<Category>> readLeavesOfRoot(@PathVariable Integer id, @PathVariable int preventPrivate, @RequestAttribute Operator operator) {
        return categoryService.readLeavesOfRoot(id, preventPrivate == 0 ? false : true, operator).setOperator(operator);
    }

    @RequestMapping(value = "/direct_sub/{id}", method = RequestMethod.GET)
    public Result<List<Category>> readDirectSubCategories(@PathVariable Integer id, @RequestAttribute Operator operator) {
        return categoryService.readDirectSubCategories(id, operator).setOperator(operator);
    }

    @RequestMapping(value = "/category_tree", method = RequestMethod.GET)
    public Result readTree(@RequestAttribute Operator operator) {
        return categoryService.readCategoryTreeString().setOperator(operator);
    }

    @RequestMapping(value = "/category_map", method = RequestMethod.GET)
    public Result<Map<Integer, Category>> readCategoryMap(@RequestAttribute Operator operator) {
        return categoryService.readCategoryMap().setOperator(operator);
    }

}
