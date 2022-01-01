package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.common.BaseDao;
import com.laputa.laputa_sns.common.TmpEntry;
import com.laputa.laputa_sns.model.entity.CommentL2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JQH
 * @since 下午 1:58 20/02/29
 */

@Repository
public interface CommentL2Dao extends BaseDao<CommentL2> {
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    List<TmpEntry> selectTopNL2OfL1StrByMultiId(List<Integer> idList);
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    int updateTopNL2IdStr(@Param("l1Id") int l1Id, @Param("l2Cnt") long l2Cnt, @Param("str") String str);
    int correctLikeCnt();
    int setPopularIndexFlag(@Param("id") int id, @Param("value") int value);
}
