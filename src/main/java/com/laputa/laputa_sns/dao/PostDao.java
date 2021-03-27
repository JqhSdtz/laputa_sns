package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.common.BaseDao;
import com.laputa.laputa_sns.common.TmpEntry;
import com.laputa.laputa_sns.model.entity.Category;
import com.laputa.laputa_sns.model.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JQH
 * @since 下午 5:54 20/02/21
 */

@Repository
public interface PostDao extends BaseDao<Post> {
    int updateTopComment(@Param("postId") int postId, @Param("commentId") Integer commentId);
    int updateCategory(@Param("postId") int postId, @Param("categoryId") Integer categoryId);
    int updateContent(Post post);
    List<Post> selectList(@Param("post") Post post, @Param("category_list") List<Category> categoryList);
    int insertFullText(TmpEntry fullText);
    String selectFullText(int fullTextId);
    int correctCommentCnt();
    int correctForwardCnt();
    int correctLikeCnt();
    int setPopularIndexFlag(@Param("id") int id, @Param("value") int value);
}
