package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.model.entity.Category;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.laputa.laputa_sns.common.Result.FAIL;

/**
 * 帖子索引服务
 *
 * @author JQH
 * @since 下午 5:31 20/02/21
 */

@Slf4j
@Service
@EnableScheduling
@Order(1)
public class PostIndexService implements ApplicationRunner {

    public static final int POPULAR = 1;
    public static final int LATEST = 2;
    public static final int CREATOR = 3;
    public static final int SUP_POST = 4;

    private final CategoryService categoryService;
    private final PostService postService;
    private final CommonService commonService;

    private final Operator progOperator = ProgOperatorManager.register(PostIndexService.class);

    public PostIndexService(@NotNull @Lazy CategoryService categoryService, @Lazy PostService postService, CommonService commonService) {
        this.categoryService = categoryService;
        this.postService = postService;
        this.commonService = commonService;
    }

    /**
     * 帖子索引的最大缓存长度，超过此长度的将放入数据库
     */
    @Value("${post-index-max-cache-num}")
    private int postIndexMaxCacheNum;

    @Override
    public void run(ApplicationArguments args) {
        Category groundCategory = categoryService.readGroundCategory().getObject();
        Set<Category> leafCategorySet = categoryService.readLeafCategorySet().getObject();
        Post param = (Post) new Post().setQueryParam(new QueryParam().setStartId(0).setFrom(0)).setType(Post.TYPE_PUBLIC);
        //加载索引前，先清除数据库的索引标记，若采用多个实例，当前的实例不包含全部的帖子时，要改一下代码，加上条件
        commonService.clearIndexedFlag("lpt_post", "post_id", "post_p_indexed_flag", "post_l_indexed_flag");
        for (Category category : leafCategorySet) {
            if (category.getCacheNum() == null || category.getCacheNum() == 0)
                continue;
            //加载新帖索引
            param.setCategory(category).getQueryParam().setQueryNum(category.getCacheNum());
            Result<List<Post>> latestPostListResult = postService.readDBPostList(param, LATEST, true, false, true, progOperator);
            if (latestPostListResult.getState() == FAIL)
                continue;
            List<Post> latestPostList = latestPostListResult.getObject();
            postService.multiSetIndexedFlag(latestPostList, LATEST, 1);
            for (int i = 0; i < latestPostList.size(); ++i) {
                Post post = latestPostList.get(i);
                category.getLatestPostList().addLast(new Index(post.getId(), post.getCreateTime().getTime()), false);
            }
            //加载热帖索引
            Result<List<Post>> popularPostListResult = postService.readDBPostList(param, POPULAR, true, false, true, progOperator);
            if (popularPostListResult.getState() == FAIL)
                continue;
            List<Post> popularPostList = popularPostListResult.getObject();
            postService.multiSetIndexedFlag(popularPostList, POPULAR, 1);
            for (int i = 0; i < popularPostList.size(); ++i) {
                Post post = popularPostList.get(i);
                category.getPopularPostList().addLast(new Index(post.getId(), post.getLikeCnt()), true);
            }
        }
        loadSubPostIndexList(groundCategory, LATEST);
        loadSubPostIndexList(groundCategory, POPULAR);
        Globals.PostIndexInitialized = true;
        log.info("帖子索引加载完成");
    }

    private IndexList loadSubPostIndexList(@NotNull Category root, int type) {
        if (root.getIsLeaf() == null || root.getIsLeaf()) {
            if (type == LATEST)
                return root.getLatestPostList();
            if (type == POPULAR)
                return root.getPopularPostList();
        }
        List<Category> subCategoryList = root.getSubCategoryList();
        List<Index> totalList = new ArrayList();
        for (int i = 0; i < subCategoryList.size(); ++i) {
            Category category = subCategoryList.get(i);
            IndexList subList = loadSubPostIndexList(category, type);
            //私有目录，不向上展示，但也需要加载
            if (category.getType() != null && category.getType().equals(Category.TYPE_PRIVATE))
                continue;
            for (Iterator<Index> iter = subList.iterator(); iter.hasNext(); )
                totalList.add(iter.next());
        }
        Collections.sort(totalList);
        IndexList rootList = null;
        if (type == LATEST)
            rootList = root.getLatestPostList();
        else if (type == POPULAR)
            rootList = root.getPopularPostList();
        int limit = Math.min(totalList.size(), postIndexMaxCacheNum);
        for (int i = 0; i < limit; ++i)
            rootList.addLast(totalList.get(i), false);
        return rootList;
    }

    public List<Integer> getPostIndexList(Category category, int type, Integer startId, Integer from, int num) {
        IndexList indexList = type == LATEST ? category.getLatestPostList() : category.getPopularPostList();
        if (indexList == null)
            return null;
        if (indexList.size() == 0)
            return new ArrayList(0);
        List<Integer> postIdList = new ArrayList(num);
        Iterator<Index> iter = indexList.iterator(startId, from);
        for (int cnt = 0; iter.hasNext() && cnt < num; ++cnt)
            postIdList.add(iter.next().getId());
        return postIdList;
    }

    /**
     * category需要是完整对象, 返回数组res[0]表示添加后索引列表的最后一个节点的ID，res[1]表示添加后索引列表的长度
     */
    public int[] addPostIndex(@NotNull List<Post> postList, Category category, int type) {
        Map<Integer, TmpEntry> changeMap = new HashMap(postList.size());//用map是为了防止id重复，把有改动的记录到changeMap中
        for (int i = 0; i < postList.size(); ++i)
            addPostIndex(postList.get(i), type, false, changeMap);
        postService.multiSetIndexedFlag(new ArrayList(changeMap.values()), type);
        int[] res = new int[2];
        if (type == POPULAR) {
            res[1] = category.getPopularPostList().size();
            res[0] = res[1] == 0 ? 0 : category.getPopularPostList().getLast().getId();
        } else {
            res[1] = category.getLatestPostList().size();
            res[0] = res[1] == 0 ? 0 : category.getLatestPostList().getLast().getId();
        }
        return res;
    }

    /**
     * 更新父目录后将本目录索引刷新到父目录中
     */
    public void refreshIndexAfterUpdateParent(Category category) {
        if (category.getType().equals(Category.TYPE_PRIVATE))
            return;
        Category parent = categoryService.readCategory(category.getParentId(), false, progOperator).getObject();
        IndexList popularList = category.getPopularPostList();
        IndexList latestList = category.getLatestPostList();
        for (Iterator<Index> iter = popularList.iterator(); iter.hasNext(); )
            doUpdatePopularIndex(iter.next(), parent, true, false, new HashMap<>());
        for (Iterator<Index> iter = latestList.iterator(); iter.hasNext(); )
            doUpdateLatestIndex(iter.next(), parent, false, new HashMap<>());
    }

    public void addPostIndex(Post post, int type, boolean isNew, Map<Integer, TmpEntry> changeMap) {
        if (type == LATEST) {
            long date = isNew ? new Date().getTime() : post.getCreateTime().getTime();
            Index index = new Index(post.getId(), date);
            Category category = categoryService.readCategory(post.getCategoryId(), false, progOperator).getObject();
            doUpdateLatestIndex(index, category, isNew, changeMap);
        } else if (type == POPULAR)
            updatePostPopIndex(post, true, false, changeMap);
    }

    private void doUpdateLatestIndex(Index index, Category category, boolean isNew, Map<Integer, TmpEntry> changeMap) {
        while (category != null) {
            IndexList indexList = category.getLatestPostList();
            if (!indexList.hasIndex(index.getId())) {//没有该节点才放入
                if (indexList.size() >= postIndexMaxCacheNum) {
                    Index last = indexList.popLast();
                    if (last != null && changeMap != null)
                        changeMap.put(last.getId(), new TmpEntry(last.getId(), 0));
                }
                if (isNew)//新加入的latest索引不需要设置index_flag，见PostService.createComment
                    indexList.addFirst(index, false);
                else {
                    Index last = indexList.addLast(index, true);
                    if (last != null && changeMap != null)
                        changeMap.put(last.getId(), new TmpEntry(last.getId(), 1));
                }
            }
            if (category.getType() != null && category.getType().equals(Category.TYPE_PRIVATE))//私有目录，不向上展示
                break;
            category = category.getParent();
        }
    }

    public boolean updatePostPopIndex(@NotNull Post post, boolean incr, boolean onlyIfExistsOrGreaterThanLast, Map<Integer, TmpEntry> changeMap) {
        Index index = new Index(post.getId(), post.getLikeCnt());
        Category category = categoryService.readCategory(post.getCategoryId(), false, progOperator).getObject();
        return doUpdatePopularIndex(index, category, incr, onlyIfExistsOrGreaterThanLast, changeMap);
    }

    private boolean doUpdatePopularIndex(Index index, Category category, boolean incr, boolean onlyIfExistsOrGreaterThanLast, Map<Integer, TmpEntry> changeMap) {
        boolean success = false;
        while (category != null) {
            IndexList indexList = category.getPopularPostList();
            if (incr) {//增加热度
                boolean greaterThanLast = indexList.getLast() == null || index.getValue() > indexList.getLast().getValue();
                if (indexList.hasIndex(index.getId())) {//原来在列表中
                    success = indexList.update(index, true, IndexList.LEFT);
                } else if (!onlyIfExistsOrGreaterThanLast || greaterThanLast) {
                    //不是只更新存在或插入更大的情况，或者是当前值比末尾更大，都继续执行
                    if (indexList.size() < postIndexMaxCacheNum) {//不在列表中但列表还有空间
                        Index last = indexList.addLast(index, true);
                        if (last != null) {
                            success = true;
                            if (changeMap != null)
                                changeMap.put(last.getId(), new TmpEntry(last.getId(), 1));
                        }
                    } else {
                        if (greaterThanLast) {//不在列表中且没有空间，判断要不要换掉末端帖子
                            Index popped = indexList.popLast();
                            if (popped != null && changeMap != null)
                                changeMap.put(popped.getId(), new TmpEntry(popped.getId(), 0));
                            Index added = indexList.addLast(index, true);
                            if (added != null) {
                                success = true;
                                if (changeMap != null)
                                    changeMap.put(added.getId(), new TmpEntry(added.getId(), 1));
                            }
                        }
//                        下层的没有是否代表上层也没有？目前来说，如果下层长度限制小，上层长度限制大，下层没有的也可能到上层
//                        而且多判断几次对性能损耗不大，所以不提前退出
//                        else
//                           break;
                    }
                }
            } else {//减少热度
                if (indexList.hasIndex(index.getId())) {//原来在列表中
                    indexList.update(index, true, IndexList.RIGHT);
                    success = true;
                }
//                同上
//                else
//                   break;
            }
            if (category.getType() != null && category.getType().equals(Category.TYPE_PRIVATE))//私有目录，不向上展示
                break;
            category = category.getParent();
        }
        return success;
    }

    public void deletePostIndex(@NotNull Post post, int type) {
        Category category = categoryService.readCategory(post.getCategoryId(), false, progOperator).getObject();
        while (category != null) {
            if (type == LATEST)
                category.getLatestPostList().remove(post.getId());
            else if (type == POPULAR)
                category.getPopularPostList().remove(post.getId());
//            if (res == null)//子目录索引没有的，父目录索引必定没有，不用再往上找了
//                break;
            if (category.getType() != null && category.getType().equals(Category.TYPE_PRIVATE))//私有目录，不向上展示
                break;
            category = category.getParent();
        }
    }

    @Scheduled(cron = "0 50 3 * * ?")
    public void dailyFlushPostIndex() {
        //把目录的索引列表长度减到默认缓存长度
        List<TmpEntry> remainedPopEntryList = new ArrayList();
        List<TmpEntry> remainedLatEntryList = new ArrayList();
        Set<Category> leafSet = categoryService.readLeafCategorySet().getObject();
        for (Category category : leafSet) {
            // 每次刷新时把所有叶目录的缓存长度改成10，可以一定程度上削弱顶端优势
            // 此种设计仍有待验证
            List<Index> remainedPopList = category.getPopularPostList().trim(10, true);
            List<Index> remainedLatList = category.getLatestPostList().trim(10, true);
            for (int i = 0; i < remainedPopList.size(); ++i)
                remainedPopEntryList.add(new TmpEntry(remainedPopList.get(i).getId(), 1));
            for (int i = 0; i < remainedLatList.size(); ++i)
                remainedLatEntryList.add(new TmpEntry(remainedLatList.get(i).getId(), 1));
        }
        commonService.clearIndexedFlag("lpt_post", "post_id", "post_p_indexed_flag", "post_l_indexed_flag");
        postService.multiSetIndexedFlag(remainedPopEntryList, POPULAR);
        postService.multiSetIndexedFlag(remainedLatEntryList, LATEST);
        Map<Integer, Category> categoryMap = categoryService.readCategoryMap().getObject();
        for (Category category : categoryMap.values()) {
            if (!category.getIsLeaf()) {
                category.setPopularPostList(new IndexList(10));
                category.setLatestPostList(new IndexList(10));
            }
        }
        Category groundCategory = categoryService.readGroundCategory().getObject();
        loadSubPostIndexList(groundCategory, LATEST);
        loadSubPostIndexList(groundCategory, POPULAR);
        log.info("目录索引列表以缩减到默认缓存长度");
        List<TmpEntry> entryList = new ArrayList(categoryMap.size());
        for (Category category : categoryMap.values()) {
            int cacheNum = Math.max(category.getPopularCacheNum(), category.getLatestCacheNum());
            cacheNum = Math.max(cacheNum, postIndexMaxCacheNum);
            entryList.add(new TmpEntry(category.getId(), cacheNum));
        }
        commonService.batchUpdate("lpt_category", "category_id", "category_cache_num", CommonService.OPS_COPY, entryList);
        log.info("目录索引列表预缓存值更新成功" + entryList.size() + "条");
    }

}
