package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.dao.SearchDao;
import com.laputa.laputa_sns.model.entity.Category;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.model.entity.User;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 搜索服务，搜索使用MySQL的全文索引功能
 *
 * @author JQH
 * @since 下午 12:49 20/05/14
 */

@Service
public class SearchService {

    private final SearchDao dao;
    private final CategoryService categoryService;
    private final UserService userService;

    /**
     * 搜索结果只返回20个
     */
    @Value("${search-result-limit}")
    private int searchResultLimit;

    public SearchService(SearchDao dao, CategoryService categoryService, UserService userService) {
        this.dao = dao;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    private String processWords(String words) {//删除通配符
        words = words.trim();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < words.length(); ++i) {
            if (words.charAt(i) != '*') {
                stringBuilder.append(words.charAt(i));
            }
        }
        return stringBuilder.toString();
    }

    @SneakyThrows
    public Result<List<Post>> searchPost(String words, String mode, Operator operator) {
        int len = words.length();
        if (len < 1 || len > 40) {
            return new Result<List<Post>>(Result.FAIL).setErrorCode(1010200201).setMessage("搜索内容长度应在1-40之间");
        }
        words = processWords(words);
        List<Post> postList = dao.searchPost(words, mode, searchResultLimit);
        userService.multiSetUser(postList, Post.class.getMethod("getCreatorId"), Post.class.getMethod("setCreator", User.class));
        categoryService.multiSetCategory(postList, Post.class.getMethod("getCategoryId"), Post.class.getMethod("setCategory", Category.class), operator);
        return new Result<List<Post>>(Result.SUCCESS).setObject(postList);
    }

    public Result<List<Category>> searchCategory(String words, String mode, Operator operator) {
        int len = words.length();
        if (len < 1 || len > 40) {
            return new Result<List<Category>>(Result.FAIL).setErrorCode(1010200201).setMessage("搜索内容长度应在1-40之间");
        }
        words = processWords(words);
        List<Category> categoryList = dao.searchCategory(words, mode, searchResultLimit);
        for (int i = 0; i < categoryList.size(); ++i) {
            categoryList.set(i, categoryService.readCategory(categoryList.get(i).getId(), false, operator).getObject());
        }
        return new Result<List<Category>>(Result.SUCCESS).setObject(categoryList);
    }

    public Result<List<User>> searchUser(String words, String mode, Operator operator) {
        int len = words.length();
        if (len < 1 || len > 40) {
            return new Result<List<User>>(Result.FAIL).setErrorCode(1010200201).setMessage("搜索内容长度应在1-40之间");
        }
        words = processWords(words);
        return new Result<List<User>>(Result.SUCCESS).setObject(dao.searchUser(words, mode, searchResultLimit));
    }
}
