package com.laputa.laputa_sns.view;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Category;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.CategoryService;
import com.laputa.laputa_sns.validator.CategoryValidator;
import com.laputa.laputa_sns.vo.CategoryVo;
import org.springframework.stereotype.Service;

/**
 * 用于读取封装后的目录对象
 * @author JQH
 * @since 下午 5:31 20/03/31
 */

@Service
public class CategoryView {
    private final CategoryService categoryService;
    private final CategoryValidator validator;

    public CategoryView(CategoryService categoryService, CategoryValidator validator) {
        this.categoryService = categoryService;
        this.validator = validator;
    }

    public Result<CategoryVo> readCategoryVo(Integer categoryId, Operator operator) {
        Result<Category> categoryResult = categoryService.readCategoryWithAllCounters(categoryId, operator);
        if (categoryResult.getState() == Result.FAIL) {
            return new Result<CategoryVo>(categoryResult);
        }
        CategoryVo vo = new CategoryVo(categoryResult.getObject());
        validator.setRights(vo, operator);
        return new Result<CategoryVo>(Result.SUCCESS).setObject(vo);
    }
}
