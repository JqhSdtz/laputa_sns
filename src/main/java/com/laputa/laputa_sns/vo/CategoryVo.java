package com.laputa.laputa_sns.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.model.Category;
import com.laputa.laputa_sns.right.CategoryRight;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author JQH
 * @since 下午 5:21 20/03/31
 */

@Getter
@Setter
public class CategoryVo {
    private Category root;
    @JsonProperty("sub_list")
    private List<Category> subList;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CategoryRight rights;

    public CategoryVo(@NotNull Category category) {
        this.root = category;
        this.subList = category.getSubCategoryList();
    }
}
