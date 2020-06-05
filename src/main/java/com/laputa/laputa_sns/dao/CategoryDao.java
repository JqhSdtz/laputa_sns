package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.common.BaseDao;
import com.laputa.laputa_sns.model.Category;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JQH
 * @since 下午 6:13 20/02/05
 */

@Repository
public interface CategoryDao extends BaseDao<Category> {
    int updateTopPost(@Param("categoryId") int categoryId, @Param("postId") Integer postId);
    int updateDefSub(@Param("categoryId") int categoryId, @Param("defSubId") int defSubId);
    int correctPostCnt(List<Category> categoryList);
}
