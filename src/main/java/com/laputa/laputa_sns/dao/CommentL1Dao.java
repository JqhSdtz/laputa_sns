package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.common.BaseDao;
import com.laputa.laputa_sns.model.entity.CommentL1;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * @author JQH
 * @since 上午 11:43 20/02/27
 */

@Repository
public interface CommentL1Dao extends BaseDao<CommentL1> {
    int correctL2Cnt();
    int correctPosterRepCnt();
    int correctLikeCnt();
    int setPopularIndexFlag(@Param("id") int id, @Param("value") int value);
}
