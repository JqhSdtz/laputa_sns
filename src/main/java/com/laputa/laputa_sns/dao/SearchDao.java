package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.model.Category;
import com.laputa.laputa_sns.model.Post;
import com.laputa.laputa_sns.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JQH
 * @since 下午 12:39 20/05/14
 */

@Repository
public interface SearchDao {
    List<Post> searchPost(@Param("words") String words, @Param("mode") String mode, @Param("limit") int limit);
    List<Category> searchCategory(@Param("words") String words, @Param("mode") String mode, @Param("limit") int limit);
    List<User> searchUser(@Param("words") String words, @Param("mode") String mode, @Param("limit") int limit);
}
