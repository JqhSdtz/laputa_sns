package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.dao.PostDao;
import com.laputa.laputa_sns.executor.IndexExecutor;
import com.laputa.laputa_sns.helper.CommentServiceHelper;
import com.laputa.laputa_sns.helper.QueryHelper;
import com.laputa.laputa_sns.helper.RedisHelper;
import com.laputa.laputa_sns.helper.SimpleQueryHelper;
import com.laputa.laputa_sns.model.entity.*;
import com.laputa.laputa_sns.util.CryptUtil;
import com.laputa.laputa_sns.util.QueryTokenUtil;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.validator.PostValidator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;
import static com.laputa.laputa_sns.service.PostIndexService.*;

/**
 * 帖子服务
 *
 * @author JQH
 * @since 下午 6:22 20/02/21
 */

@Slf4j
@EnableScheduling
@Service
public class PostService extends BaseService<PostDao, Post> {

    private final PostIndexService postIndexService;
    private final CategoryService categoryService;
    private final LikeRecordService likeRecordService;
    private final ForwardService forwardService;
    private final UserService userService;
    private final CommentL1Service commentL1Service;
    private final PostNewsService postNewsService;
    private final AdminOpsService adminOpsService;
    private final CommonService commonService;
    private final StringRedisTemplate redisTemplate;
    private final PostValidator postValidator;
    private final RedisHelper<Post> redisHelper;
    private final QueryHelper<Post> queryHelper;
    private final SimpleQueryHelper fullTextQueryHelper;
    private final IndexExecutor.CallBacks indexOfCategoryExecutorCallBacks;
    private final IndexExecutor.CallBacks indexOfCreatorExecutorCallBacks;
    private final ObjectMapper fullObjectMapper = new ObjectMapper();

    private String hmacKey;

    public PostService(@Lazy PostIndexService postIndexService, @Lazy CategoryService categoryService, @Lazy LikeRecordService likeRecordService, @Lazy ForwardService forwardService, @Lazy CommentL1Service commentL1Service, @Lazy UserService userService, @Lazy PostNewsService postNewsService, AdminOpsService adminOpsService, StringRedisTemplate redisTemplate, PostValidator postValidator, @NotNull Environment environment, CommonService commonService) {
        this.postIndexService = postIndexService;
        this.categoryService = categoryService;
        this.likeRecordService = likeRecordService;
        this.forwardService = forwardService;
        this.userService = userService;
        this.commentL1Service = commentL1Service;
        this.postNewsService = postNewsService;
        this.adminOpsService = adminOpsService;
        this.redisTemplate = redisTemplate;
        this.postValidator = postValidator;
        this.commonService = commonService;
        this.hmacKey = CryptUtil.randUrlSafeStr(64, true);
        ObjectMapper basicMapper = new ObjectMapper();
        basicMapper.setFilterProvider(new SimpleFilterProvider().addFilter("PostFilter", SimpleBeanPropertyFilter
                .filterOutAllExcept("full_text_id", "category_id", "allow_forward", "sup_id", "ori_id", "like_cnt", "comment_cnt", "view_cnt", "forward_cnt", "create_time", "type", "top_comment_id", "creator_id")));
        ObjectMapper contentMapper = new ObjectMapper();
        contentMapper.setFilterProvider(new SimpleFilterProvider().addFilter("PostFilter", SimpleBeanPropertyFilter
                .filterOutAllExcept("title", "content", "raw_img")));
        this.fullObjectMapper.setFilterProvider(new SimpleFilterProvider()
                .addFilter("PostFilter", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("UserFilter", SimpleBeanPropertyFilter.filterOutAllExcept("nick_name", "raw_avatar", "type", "state")));
        int basicRedisTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.post.basic"));
        int contentRedisTimeOut = Integer.valueOf(environment.getProperty("timeout.redis.post.content"));
        this.redisHelper = new RedisHelper(RedisPrefix.POST_BASIC, basicMapper, basicRedisTimeOut, RedisPrefix.POST_CONTENT, contentMapper, contentRedisTimeOut, RedisPrefix.POST_COUNTER, Post.class, redisTemplate);
        this.queryHelper = new QueryHelper(this, redisHelper);
        this.fullTextQueryHelper = new SimpleQueryHelper(redisTemplate, RedisPrefix.POST_FULL_TEXT, contentRedisTimeOut);
        this.fullTextQueryHelper.selectOneCallBack = id -> dao.selectFullText(id);
        this.indexOfCategoryExecutorCallBacks = initIndexOfCategoryExecutorCallBacks();
        this.indexOfCreatorExecutorCallBacks = initIndexOfCreatorExecutorCallBacks();
    }

    // 超过此长度的帖子会被当作长文章，这个值最大不能超过数据库lpt_post表的content字段的长度
    @Value("${brief-post-length-limit}")
    private int briefPostLengthLimit;

    // 用户动态发件箱最大长度
    @Value("${user-news-out-box-length}")
    private int userNewsOutBoxLength;

    // 管理员操作公开目录
    @Value("${admin-ops-record-category}")
    private int adminOpsRecordCategoryId;

    // 管理员操作发布用户
    @Value("${admin-ops-record-user}")
    private int adminOpsRecordUserId;

    private List<Post> selectList(Post post, List<Category> categoryList) {
        return dao.selectList(post, categoryList);
    }

    public void incrLikeCnt(Post post, int delta) {
        post.setLikeCnt(post.getLikeCnt() + delta);
        if (delta > 0) {
            Map<Integer, TmpEntry> tmpMap = new HashMap(1);
            postIndexService.updatePostPopIndex(post, true, true, tmpMap);
            if (tmpMap.size() == 1)
                setPopularIndexFlag(post.getId(), 1);
        } else
            postIndexService.updatePostPopIndex(post, false, true, null);
    }

    private int setPopularIndexFlag(int id, int value) {
        return dao.setPopularIndexFlag(id, value);
    }

    public Post selectOne(Post post) {
        Post res = dao.selectOne(post);
        if (res != null && res.getFullText() != null)
            fullTextQueryHelper.setRedisValue(post.getFullTextId(), res.getFullText());
        return res;
    }

    public int insertOne(@NotNull Post post) {
        if (post.getFullText() != null) {
            // 手动设置全文
            TmpEntry fullText = new TmpEntry(null, post.getFullText());
            int row = dao.insertFullText(fullText);
            if (row == -1)
                return -1;
            post.setFullTextId(fullText.getId());
            if (post.getContent().length() > briefPostLengthLimit) {
                // 手动设置全文的情况下，内容大于长度限制直接截取
                post.setContent(post.getContent().substring(0, briefPostLengthLimit));
            }
        } else if (post.getContent().length() > briefPostLengthLimit) {
            // 自动设置全文
            TmpEntry fullText = new TmpEntry(null, post.getContent());
            int row = dao.insertFullText(fullText);
            if (row == -1)
                return -1;
            post.setFullTextId(fullText.getId());
            post.setContent(post.getContent().substring(0, briefPostLengthLimit));
        }
        int res = dao.insertOne(post);
        return res == 0 ? -1 : post.getId();
    }

    /**
     * 设置置顶评论，返回1表示成功，0表示失败
     */
    private int updateTopComment(int postId, Integer commentId) {
        return dao.updateTopComment(postId, commentId);
    }

    public void updateCommentCnt(Integer postId, Long delta) {
        redisHelper.updateCounter(postId, "cm", delta);
    }

    public void updateForwardCnt(Integer postId, Long delta) {
        redisHelper.updateCounter(postId, "fw", delta);
    }

    public Result<String> readFullText(Integer fullTextId) {
        return fullTextQueryHelper.readValue(fullTextId);
    }

    /**
     * 根据Redis中的内容设置帖子的点赞数和评论数
     */
    private void setCounter(@NotNull Post post) {
        likeRecordService.setLikeCnt(post, LikeRecord.TYPE_POST);
        Long[] v = redisHelper.getRedisCounterCnt(post.getId(), "cm", "fw");
        if (v == null)
            return;
        if (v[0] != null)
            post.setCommentCnt(post.getCommentCnt() + v[0]);
        if (v[1] != null)
            post.setForwardCnt(post.getForwardCnt() + v[1]);
    }

    private void multiSetCounter(@NotNull List<Post> postList) {
        likeRecordService.multiSetLikeCnt(postList, LikeRecord.TYPE_POST);
        List<List<String>> vList = redisHelper.multiGetRedisCounterCnt(postList, "cm", "fw");
        for (int i = 0; i < postList.size(); ++i) {
            Post post = postList.get(i);
            if (post == null)
                continue;
            List<String> v = vList.get(i);
            if (v.get(0) != null)
                post.setCommentCnt(post.getCommentCnt() + Integer.valueOf(v.get(0)));
            if (v.get(1) != null)
                post.setForwardCnt(post.getForwardCnt() + Integer.valueOf(v.get(1)));
        }
    }

    /**
     * 设置帖子的创建者
     */
    private void setPostCreator(@NotNull Post post, Operator operator) {
        Result<User> userResult = userService.readUserWithAllFalse(post.getCreator().getId(), operator);
        if (userResult.getState() == SUCCESS)
            post.setCreator(userResult.getObject());
    }

    @SneakyThrows
    private void multiSetPostCategoryInfo(@NotNull List<Post> postList, Operator operator) {
        categoryService.multiSetCategory(postList, Post.class.getMethod("getCategoryId"), Post.class.getMethod("setCategory", Category.class), operator);
    }

    /**
     * 处理帖子的内容信息，目前只有管理员记录帖子的区别
     *
     * @param post
     */
    private void processPayload(Post post) {
        adminOpsService.processPost(post);
    }

    public boolean existPost(Integer id) {
        return queryHelper.existEntity(new Post(id));
    }

    public Result<List<Post>> readDBPostList(@NotNull Post post, Integer type, boolean isFull, boolean useIndexFlag, boolean withCounter, Operator operator) {
        List<Category> categoryIdList = null;
        if (post.getType() != null && post.getType().equals(Post.TYPE_PUBLIC) && post.getCategoryId() != null
                && !post.getCategory().getIsLeaf()) {
            categoryIdList = categoryService.readLeavesOfRoot(post.getCategoryId(), true, operator).getObject();
            if (categoryIdList.size() == 0)
                categoryIdList = null;
        }
        if (isFull)
            post.getQueryParam().setQueryType(QueryParam.FULL);
        if (type != null) {
            String orderBy;
            if (type.equals(CREATOR))
                orderBy = "id";
            else
                orderBy = type.equals(LATEST) ? "create_time" : "like_cnt";
            post.getQueryParam().setIsPaged(1).setOrderBy(orderBy).setOrderDir("desc");
        }
        int oriFrom = post.getQueryParam().getFrom();
        if (useIndexFlag)
            post.getQueryParam().setQueryNotIndexed(type == POPULAR ? 'p' : 'l').setFrom(0);
        if (!useIndexFlag && "id".equals(post.getQueryParam().getOrderBy()) && !post.getQueryParam().getStartId().equals(0))
            post.getQueryParam().setUseStartIdAtSql(1);
        List<Post> postList = selectList(post, categoryIdList);
        if (useIndexFlag)
            post.getQueryParam().setFrom(oriFrom);
        if (postList == null)
            return new Result(FAIL).setErrorCode(1010050101).setMessage("数据库操作失败");
        redisHelper.multiSetAndRefreshEntity(postList, null, isFull);
        postList.forEach(this::processPayload);
        if (withCounter)//redis中存的对象是不带counter的，所以要在放入Redis之后设置counter
            multiSetCounter(postList);
        return new Result(SUCCESS).setObject(postList);
    }

    @NotNull
    private IndexExecutor.CallBacks initIndexOfCategoryExecutorCallBacks() {
        IndexExecutor.CallBacks<Post> callBacks = new IndexExecutor.CallBacks();
        callBacks.getIdListCallBack = (executor) -> {
            IndexExecutor.Param param = executor.param;
            QueryParam queryParam = param.paramEntity.getQueryParam();
            param.idList = postIndexService.getPostIndexList(((Post) param.paramEntity).getCategory(), param.type,
                    queryParam.getStartId(), queryParam.getFrom(), queryParam.getQueryNum());
        };
        callBacks.multiReadEntityCallBack = (executor) -> {
            Result<List<Post>> postListResult = queryHelper.multiReadEntity(executor.param.idList, true, new Post());
            if (postListResult.getState() == FAIL)
                return postListResult;
            multiSetCounter(postListResult.getObject());//设置counter
            return postListResult;
        };
        callBacks.getDBListCallBack = (executor) -> readDBPostList((Post) executor.param.paramEntity, executor.param.type, true, true, true, executor.param.operator);
        callBacks.multiSetRedisIndexCallBack = (entityList, executor) -> {
            int[] res = postIndexService.addPostIndex(entityList, ((Post) executor.param.paramEntity).getCategory(), executor.param.type);
            executor.param.newStartId = res[0];
            executor.param.newFrom = res[1];
            //帖子加入索引后可能发生排序，加入时的最后一个不一定是排序后的最后一个，所以需要单独取出来最后一个
        };
        callBacks.readOneEntityCallBack = (id, executor) -> readPostWithCounterAndContent(id, executor.param.operator);
        return callBacks;
    }


    @NotNull
    private IndexExecutor.CallBacks initIndexOfCreatorExecutorCallBacks() {
        IndexExecutor.CallBacks<Post> callBacks = new IndexExecutor.CallBacks();
        callBacks.getIdListCallBack = executor -> {
            Post param = (Post) executor.param.paramEntity;
            if (param.getQueryParam().getFrom() >= userNewsOutBoxLength)
                executor.param.idList = new ArrayList(0);
            else
                executor.param.idList = postNewsService.readNewsPostIdListOfCreator(param.getCreatorId(), param.getQueryParam()).getObject();
        };
        callBacks.multiReadEntityCallBack = (executor) -> {
            Result<List<Post>> postListResult = queryHelper.multiReadEntity(executor.param.idList, true, (Post) executor.param.queryEntity);
            if (postListResult.getState() == FAIL)
                return postListResult;
            multiSetCounter(postListResult.getObject());
            return postListResult;
        };
        callBacks.getDBListCallBack = executor -> {
            Result<List<Post>> dbListResult = readDBPostList((Post) executor.param.paramEntity, CREATOR, true, false, true, executor.param.operator);
            if (dbListResult.getState() == FAIL)
                return dbListResult;
            return dbListResult;
        };
        callBacks.readOneEntityCallBack = (id, executor) -> readPostWithCounterAndContent(id, executor.param.operator);
        return callBacks;
    }

    /**
     * 获取帖子索引，参数中应包含categoryId和queryParam，type表示类型，包括最新和最热
     */
    @SneakyThrows
    public Result<List<Post>> readIndexPostList(@NotNull Post paramPost, int type, Operator operator) {
        if (!paramPost.isValidReadIndexOfCategoryParam(true))
            return new Result(FAIL).setErrorCode(1010050213).setMessage("操作错误，参数不合法");
        Result validateTokenResult = QueryTokenUtil.validateTokenAndSetQueryParam(paramPost, type, hmacKey);
        if (validateTokenResult.getState() == FAIL)
            return validateTokenResult;
        Result<Category> categoryResult = categoryService.readCategory(paramPost.getCategoryId(), true, operator);
        if (categoryResult.getState() == FAIL)
            return (Result) categoryResult;
        //第一次获取该目录的帖子列表，并且该目录不是首页，则将该目录推入用户最近访问列表
        if (paramPost.getQueryParam().getFrom().equals(0) && !paramPost.getCategoryId().equals(CategoryService.GROUND_ID))
            userService.pushUserRecentVisitCategory(operator.getUserId(), paramPost.getCategoryId());
        paramPost.setCategory(categoryResult.getObject()).setOfType(Post.OF_CATEGORY).setType(Post.TYPE_PUBLIC);
        Post queryEntity = (Post) new Post().setQueryParam(new QueryParam());
        IndexExecutor indexExecutor = new IndexExecutor(paramPost, queryEntity, type, indexOfCategoryExecutorCallBacks, operator);
        indexExecutor.param.childNumOfParent = categoryResult.getObject().getPostCnt();
        indexExecutor.param.topId = categoryResult.getObject().getTopPostId();
        Result<List<Post>> postListResult = indexExecutor.doIndex();
        if (postListResult.getState() == FAIL)
            return postListResult;
        List<Post> postList = postListResult.getObject();
        userService.multiSetUser(postList, Post.class.getMethod("getCreatorId"), Post.class.getMethod("setCreator", User.class));
        multiSetPostCategoryInfo(postList, operator);
        likeRecordService.multiSetIsLikedByViewer(postList, LikeRecord.TYPE_POST, operator.getUserId());
        postValidator.multiSetRights(paramPost, postList, operator);
        //设置新的位置token
        String newToken = QueryTokenUtil.generateQueryToken(paramPost, postList, queryEntity.getQueryParam(), type, hmacKey);
        return new Result(SUCCESS).setObject(postList).setAttachedToken(newToken);
    }

    public Result<List<Post>> readPostListOfCreator(@NotNull Post param, Operator operator) {
        param.setOfType(Post.OF_CREATOR);
        if (!param.isValidReadIndexOfCreatorParam(true))
            return new Result(FAIL).setErrorCode(1010050221).setMessage("操作错误，参数不合法");
        Result validateTokenResult = QueryTokenUtil.validateTokenAndSetQueryParam(param, CREATOR, hmacKey);
        if (validateTokenResult.getState() == FAIL)
            return validateTokenResult;
        Result<User> creatorResult = userService.readUserWithCounter(param.getCreatorId(), operator);
        if (creatorResult.getState() == FAIL)
            return (Result<List<Post>>) (Result) creatorResult;
        Post queryEntity = (Post) new Post().setQueryParam(new QueryParam());
        IndexExecutor indexExecutor = new IndexExecutor(param, queryEntity, CREATOR, indexOfCreatorExecutorCallBacks, operator);
        //indexExecutor.param.disablePatchFromDB = true;
        indexExecutor.param.childNumOfParent = creatorResult.getObject().getPostCnt();
        indexExecutor.param.topId = creatorResult.getObject().getTopPostId();
        Result<List<Post>> postListResult = indexExecutor.doIndex();
        if (postListResult.getState() == FAIL)
            return postListResult;
        List<Post> postList = postListResult.getObject();
        for (int i = 0; i < postList.size(); ++i)
            if (postList.get(i) != null)
                postList.get(i).setCreator(creatorResult.getObject());
        multiSetPostCategoryInfo(postList, operator);
        likeRecordService.multiSetIsLikedByViewer(postList, LikeRecord.TYPE_POST, operator.getUserId());
        postValidator.multiSetRights(param, postList, operator);
        multiSetOriPost(postList, operator);
        String newToken = QueryTokenUtil.generateQueryToken(param, postList, null, CREATOR, hmacKey);
        return new Result(SUCCESS).setObject(postList).setAttachedToken(newToken);
    }

    public void multiSetIndexedFlag(@NotNull List<Post> postList, int type, Integer flag) {
        List<TmpEntry> entryList = new ArrayList(postList.size());
        for (int i = 0; i < postList.size(); ++i)
            entryList.add(new TmpEntry(postList.get(i).getId(), flag));
        commonService.batchUpdate("lpt_post", "post_id", "post_" + (type == POPULAR ? "p" : "l") + "_indexed_flag", CommonService.OPS_COPY, entryList);
    }

    public void multiSetIndexedFlag(@NotNull List<TmpEntry> entryList, int type) {
        commonService.batchUpdate("lpt_post", "post_id", "post_" + (type == POPULAR ? "p" : "l") + "_indexed_flag", CommonService.OPS_COPY, entryList);
    }

    @NotNull
    private Result<Post> readOnePost(Integer postId, boolean withCreator, boolean withCounter, boolean withCategoryInfo, boolean withContent, boolean withFullText, Operator operator) {
        Post queryEntity = (Post) new Post(postId).setQueryParam(new QueryParam());
        if (withFullText)
            queryEntity.setQueryFullText(1);
        Result<Post> postResult = queryHelper.readEntity(queryEntity, withContent);
        if (postResult.getState() == FAIL)
            return postResult;
        Post resPost = postResult.getObject();
        if (withFullText && resPost.getFullTextId() != null && (resPost.getFromDB() == null || !resPost.getFromDB()))//从Redis中取出来的且需要全文
            resPost.setFullText(fullTextQueryHelper.readValue(resPost.getFullTextId()).getObject());
        if (withCreator)
            setPostCreator(resPost, operator);
        if (withCategoryInfo && resPost.getCategoryId() != null) {
            Result<Category> categoryResult = categoryService.readCategory(resPost.getCategoryId(), false, operator);
            if (categoryResult.getState() == SUCCESS)
                resPost.setCategory(categoryResult.getObject());
            else//没获取到目录信息，可能该目录已删除
                resPost.getCategory().setIsLeaf(true);
        }
        if (withCounter)
            setCounter(resPost);
        if (resPost.getType().equals(Post.TYPE_FORWARD) && resPost.getOriId() != null && withContent) {
            Post ori = queryHelper.readEntity(new Post(resPost.getOriId()), true).getObject();
            if (ori != null)
                resPost.setOriPost(ori);
        }
        processPayload(resPost);
        return postResult;
    }

    public Result<Post> readPostWithAllFalse(Integer postId, Operator operator) {
        return readPost(postId, false, false, false, false, false, false, 0, false, operator);
    }

    public Result<Post> readPostWithCounter(Integer postId, Operator operator) {
        return readPost(postId, false, true, false, false, false, false, 0, false, operator);
    }

    public Result<Post> readPostWithCounterAndContent(Integer postId, Operator operator) {
        return readPost(postId, false, true, false, true, false, false, 0, false, operator);
    }

    public Result<Post> readPostWithCategoryInfo(Integer postId, Operator operator) {
        return readPost(postId, false, false, true, false, false, false, 0, false, operator);
    }

    public Result<Post> readPostWithCategoryInfoAndContent(Integer postId, Operator operator) {
        return readPost(postId, false, false, true, true, false, false, 0, false, operator);
    }

    /**
     * 获取帖子
     */
    public Result<Post> readPost(Integer postId, boolean withCreator, boolean withCounter, boolean withCategoryInfo, boolean withContent, boolean withFullText, boolean withIsLikedByViewer, Integer withPreviewCommentNum, boolean withRightInfo, Operator operator) {
        if (postId == null)
            return new Result(FAIL).setErrorCode(1010050219).setMessage("ID不能为空");
        Result<Post> result = readOnePost(postId, withCreator, withCounter, withCategoryInfo, withContent, withFullText, operator);
        if (result.getState() == FAIL)
            return result;
        Post post = result.getObject();
        if (post.getType() != null && post.getType().equals(Post.TYPE_FORWARD) && withContent) {
            Result<Post> oriResult = readOnePost(post.getOriId(), withCreator, withCounter, withCategoryInfo, true, withFullText, operator);
            post.setOriPost(oriResult.getObject());
        }
        if (withPreviewCommentNum > 0) {
            CommentL1 queryEntity = (CommentL1) new CommentL1().setQueryParam(new QueryParam());
            queryEntity.setPostId(post.getId()).setQueryParam(new QueryParam().setFrom(0).setQueryNum(withPreviewCommentNum));
            post.setPreviewCommentList(commentL1Service.readIndexCommentL1List(queryEntity, CommentServiceHelper.POPULAR, true, operator).getObject());
        }
        if (withIsLikedByViewer) {
            boolean res = likeRecordService.existLikeRecordInRedis((LikeRecord) new LikeRecord().setTargetId(postId).setCreatorId(operator.getUserId()).setType(LikeRecord.TYPE_POST));
            post.setLikedByViewer(res);
        }
        if (withRightInfo)
            postValidator.setRights(post, operator, null);
        return result;
    }

    public Result<List<Post>> multiReadPostWithContentAndCounter(List<Integer> postIdList, Operator operator) {
        return multiReadPost(null, postIdList, false, true, true, true, false, false, operator);
    }

    @SneakyThrows
    public Result<List<Post>> multiReadPost(Post param, List<Integer> postIdList, boolean withCreator, boolean withCounter, boolean withCategoryInfo, boolean withContent, boolean withIsLikedByViewer, boolean withRightInfo, Operator operator) {
        Result<List<Post>> postListResult = queryHelper.multiReadEntity(postIdList, withContent, new Post());
        if (postListResult.getState() == FAIL)
            return postListResult;
        List<Post> postList = postListResult.getObject();
        if (withCreator)
            userService.multiSetUser(postList, Post.class.getMethod("getCreatorId"), Post.class.getMethod("setCreator", User.class));
        if (withCounter)
            multiSetCounter(postList);
        if (withCategoryInfo)
            multiSetPostCategoryInfo(postList, operator);
        if (withIsLikedByViewer)
            likeRecordService.multiSetIsLikedByViewer(postList, LikeRecord.TYPE_POST, operator.getUserId());
        if (withRightInfo)
            postValidator.multiSetRights(param, postList, operator);
        return postListResult;
    }

    /**
     * 给多个帖子设置原始帖子，即最开始被转发的那个帖子
     *
     * @param postList
     * @param operator
     */
    public void multiSetOriPost(@NotNull List<Post> postList, Operator operator) {
        if (postList.size() == 0)
            return;
        Map<Integer, Post> oriMap = new HashMap();
        List<Integer> oriIdList = new ArrayList();
        for (int i = 0; i < postList.size(); ++i) {
            Post post = postList.get(i);
            if (post == null)
                continue;
            if (post.getType().equals(Post.TYPE_FORWARD) && post.getOriId() != null)
                oriIdList.add(post.getOriId());
        }
        List<Post> oriList = multiReadPost(null, oriIdList, true, false, true, true, false, false, operator).getObject();
        if (oriList == null)
            return;
        for (int i = 0; i < oriList.size(); ++i)
            oriMap.put(oriList.get(i).getId(), oriList.get(i));
        for (int i = 0; i < postList.size(); ++i) {
            Post post = postList.get(i);
            if (post == null)
                continue;
            if (post.getType().equals(Post.TYPE_FORWARD) && post.getOriId() != null)
                post.setOriPost(oriMap.get(post.getOriId()));
        }
    }

    /**
     * 创建帖子
     */
    public Result<Integer> createPost(@NotNull Post post, Operator operator) {
        if (!post.isValidInsertParam())
            return new Result(FAIL).setErrorCode(1010050202).setMessage("操作错误，参数不合法");
        Category category = null;
        if (post.getType().equals(Post.TYPE_PUBLIC)) {
            if (post.getCategoryId() == adminOpsRecordCategoryId
                    && operator.getUserId() != adminOpsRecordUserId) {
                return new Result(FAIL).setErrorCode(1010050223).setMessage("该目录禁止发帖");
            }
            Result<Category> categoryResult = categoryService.readCategory(post.getCategoryId(), false, operator);
            if (categoryResult.getState() == FAIL)
                return (Result) categoryResult;
            category = categoryResult.getObject();
            if (category.getIsLeaf() != null && !category.getIsLeaf())
                return new Result(FAIL).setErrorCode(1010050211).setMessage("只能在叶目录发帖");
            post.setCategory(category);
        }
        post.setLength(post.getContent().length()).setCreator(operator.getUser());
        if (!postValidator.checkCreatePermission(post, operator))
            return new Result(FAIL).setErrorCode(1010050205).setMessage("操作失败，权限错误");
        if (post.getType().equals(Post.TYPE_PUBLIC))//与下面的addPostIndex对应
            post.setLIndexedFlag(true);
        int res = insertOne(post);
        if (res == -1)
            return new Result(FAIL).setErrorCode(1010050103).setMessage("数据库操作失败");
        post.setLikeCnt(0L).setCommentCnt(0L).setViewCnt(0L).setForwardCnt(0L);
        if (post.getType().equals(Post.TYPE_PUBLIC)) {
            categoryService.cascadeUpdatePostCnt(category.getId(), 1L);//修改目录的帖子数
            postIndexService.addPostIndex(post, LATEST, true, null);//新创建的post的latest索引标志默认为1
        }
        userService.updatePostCnt(post.getCreatorId(), 1L);
        postNewsService.pushNews(post.getCreatorId(), post.getId());
        return new Result(SUCCESS).setObject(post.getId());
    }

    /**
     * 设置置顶评论
     */
    public Result setTopComment(@NotNull Post param, boolean isCancel, Operator operator) {
        if (!isCancel && !param.isValidSetTopCommentParam())
            return new Result(FAIL).setErrorCode(1010050214).setMessage("操作错误，参数不合法");
        if (!isCancel && !param.isValidCancelTopCommentParam())
            return new Result(FAIL).setErrorCode(1010050217).setMessage("操作错误，参数不合法");
        Result<Post> postResult = queryHelper.readDBEntity(param);//设置帖子置顶评论需要判断帖子的创建者
        if (postResult.getState() == FAIL)
            return postResult;
        Post resPost = postResult.getObject();
        if (!postValidator.checkSetTopCommentPermission(resPost, operator))
            return new Result(FAIL).setErrorCode(1010050215).setMessage("操作失败，权限错误");
        Integer newCommentId = null;//取消置顶则将commentId设为空
        if (!isCancel) {
            Result<CommentL1> commentL1Result = commentL1Service.readCommentWithAllFalse(param.getTopCommentId(), operator);
            if (commentL1Result.getState() == FAIL)
                return commentL1Result;
            if (!resPost.getId().equals(commentL1Result.getObject().getPostId()))
                return new Result(FAIL).setErrorCode(1010050218).setMessage("目标评论不在本帖");
            newCommentId = param.getTopCommentId();
        }
        int res = updateTopComment(resPost.getId(), newCommentId);
        if (res == 0)
            return new Result(FAIL).setErrorCode(1010050116).setMessage("数据库操作失败");
        resPost.setTopCommentId(newCommentId);
        redisHelper.setEntity(resPost, false);
        return Result.EMPTY_SUCCESS;
    }

    /**
     * 删除帖子
     */
    @SneakyThrows
    public Result deletePost(@NotNull Post param, Operator operator) {
        if (!param.isValidDeleteParam())
            return new Result(FAIL).setErrorCode(1010050208).setMessage("操作错误，参数不合法");
        Result<Post> postResult = readPostWithCategoryInfoAndContent(param.getId(), operator);//删除帖子需要获取帖子的目录以判断权限
        if (postResult.getState() == FAIL)
            return postResult;
        Post post = postResult.getObject();
        Category category = post.getCategory();
        if (post.getType().equals(Post.TYPE_PUBLIC) && category.getIsLeaf() != null && !category.getIsLeaf())
            return new Result(FAIL).setErrorCode(1010050212).setMessage("只能在叶目录删帖");
        if (!postValidator.checkDeletePermission(post, operator))
            return new Result(FAIL).setErrorCode(1010050209).setMessage("操作失败，权限错误");
        boolean isAdminOp = !post.getCreatorId().equals(operator.getUserId());//删帖人不是发帖人，则是管理员删帖
        if (isAdminOp && !param.isValidOpComment())//管理员操作，且填写删除原因小于5个字
            return new Result(FAIL).setErrorCode(1010050222).setMessage("操作错误，操作原因字数在5-256");
        int res = deleteOne(post);
        if (res == 0)
            return new Result(FAIL).setErrorCode(1010050110).setMessage("数据库操作失败");
        redisHelper.removeEntity(post.getId());
        if (post.getType().equals(Post.TYPE_PUBLIC)) {
            categoryService.cascadeUpdatePostCnt(category.getId(), -1L);//修改目录的帖子数
            //删除帖子不需要修改indexed_flag，删除的帖子不影响数据库查询结果，评论数据同理
            postIndexService.deletePostIndex(post, LATEST);
            postIndexService.deletePostIndex(post, POPULAR);
        } else if (post.getType().equals(Post.TYPE_FORWARD)) {
            forwardService.removeFromForwardZSet(post.getSupId(), post.getId());
            updateForwardCnt(post.getSupId(), -1L);
        }
        postNewsService.deleteNews(post.getCreatorId(), post.getId());
        userService.updatePostCnt(post.getCreatorId(), -1L);
        if (isAdminOp) {
            AdminOpsRecord record = new AdminOpsRecord();
            record.setTargetId(post.getCreatorId()).setTarget(post).setDesc(fullObjectMapper.writeValueAsString(post)).setOpComment(param.getOpComment()).setType(AdminOpsRecord.TYPE_DELETE_POST);
            return adminOpsService.createAdminOpsRecord(record, operator);
        }
        return new Result(SUCCESS).setObject(post);
    }

    public String correctCounters() {
        int r1 = dao.correctCommentCnt();
        if (r1 == 0)
            return "帖子数据校正错误，请重新校正";
        int r2 = dao.correctForwardCnt();
        if (r2 == 0)
            return "帖子数据校正错误，请重新校正";
        int r3 = dao.correctLikeCnt();//不需要删redis中的点赞记录，因为还没刷到数据库中，不会被统计
        if (r3 == 0)
            return "帖子数据校正错误，请重新校正";
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getCounterKey("*")));
        return "帖子的一级评论数校正" + r1 + "条数据;帖子的转发数校正" + r2 + "条数据;帖子的点赞数校正" + r3 + "条数据";
    }

    /**
     * 每天将Redis中的帖子的评论数量记录刷入数据库
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void dailyFlushRedisToDB() {
        List<TmpEntry>[] cntLists = redisHelper.flushRedisCounter("cm", "fw");
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getBasicKey("*")));
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getContentKey("*")));
        commonService.batchUpdate("lpt_post", "post_id", "post_comment_cnt", CommonService.OPS_INCR_BY, cntLists[0]);
        commonService.batchUpdate("lpt_post", "post_id", "post_forward_cnt", CommonService.OPS_INCR_BY, cntLists[1]);
        log.info("帖子的一级评论数量写入数据库");
        hmacKey = CryptUtil.randUrlSafeStr(64, true);//更新hmac的key
    }

}
