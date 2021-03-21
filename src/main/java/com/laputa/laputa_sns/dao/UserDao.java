package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.common.BaseDao;
import com.laputa.laputa_sns.model.entity.User;
import com.laputa.laputa_sns.model.entity.UserRecvSetting;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * @author JQH
 * @since 下午 6:12 20/02/05
 */


@Repository
public interface UserDao extends BaseDao<User> {
    int updateAfterLogin(@Param("id") int id, @Param("token") String token, @Param("curTime") Date curTime);
    int updateTopPost(@Param("userId") int userId, @Param("postId") Integer postId);
    UserRecvSetting selectRecvSetting(Integer id);
    int updateRecvSetting(UserRecvSetting entity);
    int updatePassword(User user);
    int correctPostCnt();
    int correctFollowersCnt();
    int correctFollowingCnt();
    int redefineState(int id);
}
