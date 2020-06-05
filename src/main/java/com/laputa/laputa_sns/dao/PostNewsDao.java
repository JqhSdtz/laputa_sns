package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.common.BaseDao;
import com.laputa.laputa_sns.model.Follow;
import com.laputa.laputa_sns.model.Post;
import com.laputa.laputa_sns.model.PostNews;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JQH
 * @since 下午 1:41 20/03/11
 */

@Repository
public interface PostNewsDao extends BaseDao<PostNews> {
    List<Post> selectPostListOfAllUser(@Param("startId") int startId, @Param("queryNum") int queryNum);
    List<PostNews> selectList(@Param("entity") PostNews entity, @Param("followingList")List<Follow> followingList);
}
