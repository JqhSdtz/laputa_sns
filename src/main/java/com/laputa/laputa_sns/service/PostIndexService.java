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

// Category的父类AbstractBaseEntity中已经重写了hashCode和equals，所以这里没有问题
@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
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
    /**
     * 帖子索引的最大缓存长度，超过此长度的将放入数据库
     */
    @Value("${post-index-max-cache-num}")
    private int postIndexMaxCacheNum;

    public PostIndexService(@NotNull @Lazy CategoryService categoryService, @Lazy PostService postService, CommonService commonService) {
        this.categoryService = categoryService;
        this.postService = postService;
        this.commonService = commonService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Category groundCategory = categoryService.readGroundCategory().getObject();
        Set<Category> leafCategorySet = categoryService.readLeafCategorySet().getObject();
        Post param = (Post) new Post().setQueryParam(new QueryParam().setStartId(0).setFrom(0)).setType(Post.TYPE_PUBLIC);
        // 加载索引前，先清除数据库的索引标记，若采用多个实例，当前的实例不包含全部的帖子时，要改一下代码，加上条件
        commonService.clearIndexedFlag("lpt_post", "post_id", "post_p_indexed_flag", "post_l_indexed_flag");
        for (Category category : leafCategorySet) {
            if (category.getCacheNum() == null || category.getCacheNum() == 0) {
                continue;
            }
            // 加载新帖索引
            param.setCategory(category).getQueryParam().setQueryNum(category.getCacheNum());
            Result<List<Post>> latestPostListResult = postService.readDbPostList(param, LATEST, true, false, true, progOperator);
            if (latestPostListResult.getState() == FAIL) {
                continue;
            }
            List<Post> latestPostList = latestPostListResult.getObject();
            postService.multiSetIndexedFlag(latestPostList, LATEST, 1);
            for (int i = 0; i < latestPostList.size(); ++i) {
                Post post = latestPostList.get(i);
                category.getLatestPostList().addLast(new Index(post.getId(), post.getCreateTime().getTime()), false);
            }
            // 加载热帖索引
            Result<List<Post>> popularPostListResult = postService.readDbPostList(param, POPULAR, true, false, true, progOperator);
            if (popularPostListResult.getState() == FAIL) {
                continue;
            }
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

    /**
     * 以递归的方式加载该目录以及其全部子目录的帖子索引
     *
     * @param root 目标目录
     * @param type 索引类型（POPULAR, LATEST）
     * @return
     */
    private IndexList loadSubPostIndexList(@NotNull Category root, int type) {
        if (root.getIsLeaf() == null || root.getIsLeaf()) {
            if (type == LATEST) {
                return root.getLatestPostList();
            }
            if (type == POPULAR) {
                return root.getPopularPostList();
            }
        }
        List<Category> subCategoryList = root.getSubCategoryList();
        List<Index> totalList = new ArrayList<>();
        for (int i = 0; i < subCategoryList.size(); ++i) {
            Category category = subCategoryList.get(i);
            IndexList subList = loadSubPostIndexList(category, type);
            // 私有目录，不向上展示，但也需要加载
            if (category.getType() != null && category.getType().equals(Category.TYPE_PRIVATE)) {
                continue;
            }
            for (Iterator<Index> iter = subList.iterator(); iter.hasNext(); ) {
                totalList.add(iter.next());
            }
        }
        Collections.sort(totalList);
        IndexList rootList = null;
        if (type == LATEST) {
            rootList = root.getLatestPostList();
        } else if (type == POPULAR) {
            rootList = root.getPopularPostList();
        }
        int limit = Math.min(totalList.size(), postIndexMaxCacheNum);
        for (int i = 0; i < limit; ++i) {
            rootList.addLast(totalList.get(i), false);
        }
        return rootList;
    }

    /**
     * 获取帖子索引列表
     *
     * @param category 目标目录
     * @param type     索引类型（POPULAR, LATEST）
     * @param startId  查询参数中的起始ID
     * @param from     查询参数中的起始位置
     * @param num      查询参数中的查询数量
     * @return 以帖子ID表示的索引列表
     */
    public List<Integer> getPostIndexList(Category category, int type, Integer startId, Integer from, int num) {
        IndexList indexList = type == LATEST ? category.getLatestPostList() : category.getPopularPostList();
        if (indexList == null) {
            return null;
        }
        if (indexList.size() == 0) {
            return new ArrayList<>(0);
        }
        List<Integer> postIdList = new ArrayList<>(num);
        Iterator<Index> iter = indexList.iterator(startId, from);
        for (int cnt = 0; iter.hasNext() && cnt < num; ++cnt) {
            postIdList.add(iter.next().getId());
        }
        return postIdList;
    }

    /**
     * 向指定目录中添加帖子索引
     *
     * @param postList 待添加的帖子列表
     * @param category 目标目录
     * @param type     索引类型（POPULAR, LATEST）
     * @return 返回数组res[0]表示添加后索引列表的最后一个节点的ID，res[1]表示添加后索引列表的长度
     */
    public int[] addPostIndex(@NotNull List<Post> postList, Category category, int type) {
        // 用map是为了防止id重复，把有改动的记录到changeMap中
        Map<Integer, TmpEntry> changeMap = new HashMap<>(postList.size());
        for (int i = 0; i < postList.size(); ++i) {
            addPostIndex(postList.get(i), null, type, false, changeMap);
        }
        postService.multiSetIndexedFlag(new ArrayList<>(changeMap.values()), type);
        int[] res = new int[2];
        // 确保category是完整的对象，以获取索引列表
        category = categoryService.readCategory(category.getId(), false, progOperator).getObject();
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
     * 添加帖子索引，但不会修改数据库的索引字段
     *
     * @param post
     * @param category 定义添加索引的起点，如果是null表示从发帖目录开始添加
     * @param type      索引类型（POPULAR, LATEST)
     * @param isNew     是否是新创建的帖子
     * @param changeMap 用于记录在此过程中改变了索引状态的帖子（不只包括目标帖子），如果不需要获取索引状态的改变，则可传入null
     */
    public void addPostIndex(Post post, Category category, int type, boolean isNew, Map<Integer, TmpEntry> changeMap) {
        if (category == null) {
            category = categoryService.readCategory(post.getCategoryId(), false, progOperator).getObject();
        }
        if (type == LATEST) {
            long date = isNew ? System.currentTimeMillis() : post.getCreateTime().getTime();
            Index index = new Index(post.getId(), date);
            doUpdateLatestIndex(index, category, isNew, changeMap);
        } else if (type == POPULAR) {
            updatePostPopIndex(post, category,true, false, changeMap);
        }
    }

    private void doUpdateLatestIndex(Index index, Category category, boolean isNew, Map<Integer, TmpEntry> changeMap) {
        while (category != null) {
            IndexList indexList = category.getLatestPostList();
            if (!indexList.hasIndex(index.getId())) {
                // 没有该节点才放入
                if (indexList.size() >= postIndexMaxCacheNum) {
                    Index last = indexList.popLast();
                    if (last != null && changeMap != null) {
                        changeMap.put(last.getId(), new TmpEntry(last.getId(), 0));
                    }
                }
                if (isNew)
                    // 新加入的latest索引不需要设置index_flag，见PostService.createComment
                {
                    indexList.addFirst(index, false);
                } else {
                    Index last = indexList.addLast(index, true);
                    if (last != null && changeMap != null) {
                        changeMap.put(last.getId(), new TmpEntry(last.getId(), 1));
                    }
                }
            }
            // 私有目录，不向上展示
            if (category.getType() != null && category.getType().equals(Category.TYPE_PRIVATE)) {
                break;
            }
            category = category.getParent();
        }
    }

    /**
     * 更新帖子的POPULAR索引，用于点赞或取消点赞时更新索引
     *
     * @param post
     * @param category 定义索引更新的起点，如果是null表示从发帖目录开始更新
     * @param incr true表示点赞，false表示取消点赞
     * @param onlyIfExistsOrGreaterThanLast 是否只在索引列表中已存在该帖子的索引，
     *                                      或者该帖子的索引值（点赞数）大于索引列表末尾的索引的索引值时才添加该索引。
     *                                      在添加从数据库获取的帖子列表时，需要添加索引中不存在的帖子，此时可以保证索引的顺序是正确的；
     *                                      但是在点赞时如果不加判断直接将被点赞的帖子加入索引，则无法保证索引的顺序是正确的
     * @param changeMap                     用于记录在此过程中改变了索引状态的帖子（不只包括目标帖子），如果不需要获取索引状态的改变，则可传入null
     * @return 该帖子是否加入到了索引中
     */
    public boolean updatePostPopIndex(@NotNull Post post, Category category, boolean incr, boolean onlyIfExistsOrGreaterThanLast, Map<Integer, TmpEntry> changeMap) {
        Index index = new Index(post.getId(), post.getLikeCnt());
        if (category == null) {
            category = categoryService.readCategory(post.getCategoryId(), false, progOperator).getObject();
        }
        return doUpdatePopularIndex(index, category, incr, onlyIfExistsOrGreaterThanLast, changeMap);
    }

    private boolean doUpdatePopularIndex(Index index, Category category, boolean incr, boolean onlyIfExistsOrGreaterThanLast, Map<Integer, TmpEntry> changeMap) {
        boolean success = false;
        while (category != null) {
            IndexList indexList = category.getPopularPostList();
            if (incr) {
                // 增加热度
                boolean greaterThanLast = indexList.getLast() == null || index.getValue() > indexList.getLast().getValue();
                if (indexList.hasIndex(index.getId())) {
                    // 原来在列表中
                    success = indexList.update(index, true, IndexList.LEFT);
                } else if (!onlyIfExistsOrGreaterThanLast || greaterThanLast) {
                    // 不是只更新存在或插入更大的情况，或者是当前值比末尾更大，都继续执行
                    if (indexList.size() < postIndexMaxCacheNum) {
                        // 不在列表中但列表还有空间
                        Index last = indexList.addLast(index, true);
                        if (last != null) {
                            success = true;
                            if (changeMap != null) {
                                changeMap.put(last.getId(), new TmpEntry(last.getId(), 1));
                            }
                        }
                    } else {
                        if (greaterThanLast) {
                            // 不在列表中且没有空间，判断要不要换掉末端帖子
                            Index popped = indexList.popLast();
                            if (popped != null && changeMap != null) {
                                changeMap.put(popped.getId(), new TmpEntry(popped.getId(), 0));
                            }
                            Index added = indexList.addLast(index, true);
                            if (added != null) {
                                success = true;
                                if (changeMap != null) {
                                    changeMap.put(added.getId(), new TmpEntry(added.getId(), 1));
                                }
                            }
                        }
//                        下层的没有是否代表上层也没有？目前来说，如果下层长度限制小，上层长度限制大，下层没有的也可能到上层
//                        而且多判断几次对性能损耗不大，所以不提前退出
//                        else
//                           break;
                    }
                }
            } else {
                // 减少热度
                if (indexList.hasIndex(index.getId())) {
                    // 原来在列表中
                    indexList.update(index, true, IndexList.RIGHT);
                    success = true;
                }
//                同上
//                else
//                   break;
            }
            // 私有目录，不向上展示
            if (category.getType() != null && category.getType().equals(Category.TYPE_PRIVATE)) {
                break;
            }
            category = category.getParent();
        }
        return success;
    }

    /**
     * 在指定目录中删除帖子的索引
     *
     * @param postList 待删除的帖子列表
     */
    public void deletePostIndex(@NotNull List<Post> postList) {
        Map<Integer, TmpEntry> latestChangeMap = new HashMap<>(postList.size());
        Map<Integer, TmpEntry> popularChangeMap = new HashMap<>(postList.size());
        for (int i = 0; i < postList.size(); ++i) {
            deletePostIndex(postList.get(i), null, LATEST, latestChangeMap);
        }
        for (int i = 0; i < postList.size(); ++i) {
            deletePostIndex(postList.get(i), null, POPULAR, popularChangeMap);
        }
        postService.multiSetIndexedFlag(new ArrayList<>(latestChangeMap.values()), LATEST);
        postService.multiSetIndexedFlag(new ArrayList<>(popularChangeMap.values()), POPULAR);
    }

    public void deletePostIndex(@NotNull Post post, Category category, int type, Map<Integer, TmpEntry> changeMap) {
        if (category == null) {
            category = categoryService.readCategory(post.getCategoryId(), false, progOperator).getObject();
        }
        while (category != null) {
            Index removedIndex = null;
            if (type == LATEST) {
                removedIndex = category.getLatestPostList().remove(post.getId());
            } else if (type == POPULAR) {
                removedIndex = category.getPopularPostList().remove(post.getId());
            }
            if (changeMap != null && removedIndex != null) {
                changeMap.put(post.getId(), new TmpEntry(post.getId(), 0));
            }
// 目录迁移的情况下，上级目录可能是空的，但再上级可能包含需要删除的索引，这里为了简便，一律找到最上层
//            if (res == null)
//                // 子目录索引没有的，父目录索引必定没有，不用再往上找了
//                break;
            // if (category.getType() != null && category.getType().equals(Category.TYPE_PRIVATE)) {
            //     // 私有目录，不向上展示
            //     break;
            // }
            category = category.getParent();
        }
    }

    /**
     * 转移帖子的索引，从原目录转移到目标目录，需要保证帖子的点赞数和创建时间字段是正确的
     *
     * @param post     待转移的帖子
     * @param original 帖子的原目录
     * @param target   帖子的目标目录
     */
    public void transferPostIndex(Post post, Category original, Category target) {
        Map<Integer, TmpEntry> latestChangeMap = new HashMap<>(1);
        Map<Integer, TmpEntry> popularChangeMap = new HashMap<>(1);
        // 删除索引是删除的原目录的索引
        post.setCategory(original);
        deletePostIndex(post, null, LATEST, latestChangeMap);
        deletePostIndex(post, null, POPULAR, popularChangeMap);
        // 添加索引时添加的目标目录的索引
        post.setCategory(target);
        addPostIndex(post, null, LATEST, false, latestChangeMap);
        addPostIndex(post, null, POPULAR, false, popularChangeMap);
        postService.multiSetIndexedFlag(new ArrayList<>(latestChangeMap.values()), LATEST);
        postService.multiSetIndexedFlag(new ArrayList<>(popularChangeMap.values()), POPULAR);
    }

    /**
     * 更新父目录后或修改目录类型后，按照新的目录结构刷新本目录的索引列表
     * @param category 要修改的目录
     * @param delete 是否删除本目录在上级目录的索引
     * @param add 是否在上级目录添加本目录的索引
     */
    public void moveCategoryIndexList(Category category, boolean delete, boolean add) {
        if (category.getType().equals(Category.TYPE_PRIVATE)) {
            return;
        }
        IndexList latestList = category.getLatestPostList();
        IndexList popularList = category.getPopularPostList();
        Map<Integer, TmpEntry> latestChangeMap = new HashMap<>(latestList.size());
        Map<Integer, TmpEntry> popularChangeMap = new HashMap<>(popularList.size());
        List<Post> latestPostList = new ArrayList<>(popularList.size());
        List<Post> popularPostList = new ArrayList<>(popularList.size());
        // 先从索引列表中读取出来所有的数据，因为在删除的过程中索引列表会变化，所以不能遍历的同时删除
        for (Iterator<Index> iter = latestList.iterator(); iter.hasNext(); ) {
            Index index = iter.next();
            latestPostList.add((Post) new Post(index.getId()).setCategory(category).setCreateTime(new Date(index.getValue())));
        }
        for (Iterator<Index> iter = popularList.iterator(); iter.hasNext(); ) {
            Index index = iter.next();
            popularPostList.add(new Post(index.getId()).setCategory(category).setLikeCnt(index.getValue()));
        }
        // 本目录及以下的索引结构不会发生改变，以父目录作为起点修改目录索引
        Category parentCategory = category.getParent();
        if (delete) {
            for (int i = 0; i < latestPostList.size(); ++i) {
                deletePostIndex(latestPostList.get(i), parentCategory, LATEST, latestChangeMap);
            }
            for (int i = 0; i < popularPostList.size(); ++i) {
                deletePostIndex(popularPostList.get(i), parentCategory, POPULAR, popularChangeMap);
            }
        }
        if (add) {
            for (int i = 0; i < latestPostList.size(); ++i) {
                addPostIndex(latestPostList.get(i), parentCategory, LATEST, false, latestChangeMap);
            }
            for (int i = 0; i < popularPostList.size(); ++i) {
                addPostIndex(popularPostList.get(i), parentCategory, POPULAR, false, popularChangeMap);
            }
        }
        postService.multiSetIndexedFlag(new ArrayList<>(latestChangeMap.values()), LATEST);
        postService.multiSetIndexedFlag(new ArrayList<>(popularChangeMap.values()), POPULAR);
    }

    @Scheduled(cron = "0 50 3 * * ?")
    public void dailyFlushPostIndex() {
        // 把目录的索引列表长度减到默认缓存长度
        List<TmpEntry> remainedPopEntryList = new ArrayList<>();
        List<TmpEntry> remainedLatEntryList = new ArrayList<>();
        Set<Category> leafSet = categoryService.readLeafCategorySet().getObject();
        for (Category category : leafSet) {
            // 每次刷新时把所有叶目录的缓存长度改成10，可以一定程度上削弱顶端优势
            // 此种设计仍有待验证
            List<Index> remainedPopList = category.getPopularPostList().trim(10, true);
            List<Index> remainedLatList = category.getLatestPostList().trim(10, true);
            for (int i = 0; i < remainedPopList.size(); ++i) {
                remainedPopEntryList.add(new TmpEntry(remainedPopList.get(i).getId(), 1));
            }
            for (int i = 0; i < remainedLatList.size(); ++i) {
                remainedLatEntryList.add(new TmpEntry(remainedLatList.get(i).getId(), 1));
            }
        }
        commonService.clearIndexedFlag("lpt_post", "post_id", "post_p_indexed_flag", "post_l_indexed_flag");
        postService.multiSetIndexedFlag(remainedPopEntryList, POPULAR);
        postService.multiSetIndexedFlag(remainedLatEntryList, LATEST);
        Map<Integer, Category> categoryMap = categoryService.readCategoryMap().getObject();
        // 清空所有非叶目录的索引列表，然后再由叶目录的索引列表重构其他目录的索引列表，以保证结构的正确
        for (Category category : categoryMap.values()) {
            if (!category.getIsLeaf()) {
                category.setPopularPostList(new IndexList(10));
                category.setLatestPostList(new IndexList(10));
            }
        }
        Category groundCategory = categoryService.readGroundCategory().getObject();
        loadSubPostIndexList(groundCategory, LATEST);
        loadSubPostIndexList(groundCategory, POPULAR);
        log.info("目录索引列表已缩减到默认缓存长度");
        List<TmpEntry> entryList = new ArrayList<>(categoryMap.size());
        for (Category category : categoryMap.values()) {
            int cacheNum = Math.max(category.getPopularCacheNum(), category.getLatestCacheNum());
            cacheNum = Math.max(cacheNum, postIndexMaxCacheNum);
            entryList.add(new TmpEntry(category.getId(), cacheNum));
        }
        commonService.batchUpdate("lpt_category", "category_id", "category_cache_num", CommonService.OPS_COPY, entryList);
        log.info("目录索引列表预缓存值更新成功" + entryList.size() + "条");
    }

}
