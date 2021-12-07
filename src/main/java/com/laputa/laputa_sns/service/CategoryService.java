package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.common.*;
import com.laputa.laputa_sns.dao.CategoryDao;
import com.laputa.laputa_sns.helper.RedisHelper;
import com.laputa.laputa_sns.model.entity.AdminOpsRecord;
import com.laputa.laputa_sns.model.entity.Category;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.Post;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import com.laputa.laputa_sns.util.RedisUtil;
import com.laputa.laputa_sns.validator.CategoryValidator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * 目录服务
 * 注：除初始化过程外，任何需要直接改动原有Category对象的方法都应该设为同步！
 * @author JQH
 * @since 下午 8:17 20/02/06
 */

@Slf4j
@Service
@Order(0)
public class CategoryService extends BaseService<CategoryDao, Category> implements ApplicationRunner {

    public static final Integer GROUND_ID = 1;

    private Map<Integer, Category> categoryMap;
    private Set<Category> leafCategorySet;
    private Category groundCategory;

    private final PostService postService;
    private final PostIndexService postIndexService;
    private final AdminOpsService adminOpsService;
    private final CommonService commonService;
    private final CategoryValidator categoryValidator;
    private final RedisHelper<Category> redisHelper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Operator progOperator = ProgOperatorManager.register(CategoryService.class);

    public CategoryService(PostService postService, PostIndexService postIndexService, AdminOpsService adminOpsService, CommonService commonService, @Lazy CategoryValidator categoryValidator, StringRedisTemplate redisTemplate) {
        this.postService = postService;
        this.postIndexService = postIndexService;
        this.adminOpsService = adminOpsService;
        this.commonService = commonService;
        this.categoryValidator = categoryValidator;
        this.redisTemplate = redisTemplate;
        this.redisHelper = new RedisHelper<>(RedisPrefix.CATEGORY_COUNTER, redisTemplate);
        this.objectMapper.setFilterProvider(new SimpleFilterProvider()
                .addFilter("UserFilter", SimpleBeanPropertyFilter.filterOutAllExcept("nick_name", "raw_avatar", "type", "state")));
    }

    @Value("${admin-ops-record-category}")
    private int adminOpsRecordCategoryId;

    @Override
    /**加载目录树*/
    public void run(ApplicationArguments args) {
        loadCategoryFromDB(false);
    }

    private void loadCategoryFromDB(boolean fromReload) {
        List<Category> categoryList = dao.selectList(new Category());
        Map<Integer, Category> newCategoryMap  = new ConcurrentHashMap<>();
        for (int i = 0; i < categoryList.size(); ++i) {
            Category category = categoryList.get(i);
            if (fromReload && categoryMap != null) {
                Category oriCategory = categoryMap.get(category.getId());
                if (oriCategory != null) {
                    category.setPopularPostList(oriCategory.getPopularPostList());
                    category.setLatestPostList(oriCategory.getLatestPostList());
                }
            } else {
                category.setPopularPostList(new IndexList(10));
                category.setLatestPostList(new IndexList(10));
            }
            category.setAllowUserPost(category.getId() != adminOpsRecordCategoryId);
            newCategoryMap.put(category.getId(), category);
        }
        categoryMap = newCategoryMap;
        groundCategory = categoryMap.get(GROUND_ID);
        if (groundCategory == null) {
            log.warn("数据库无基础目录，将添加缺省基础目录");
            groundCategory = (Category) new Category().setName("根目录").setIsLeaf(true).setType(Category.TYPE_COMMON);
            categoryMap.put(GROUND_ID, groundCategory);
            int res = insertOne(groundCategory);
            if (res == -1)
                log.error("添加缺省目录失败，请检查数据库并手动添加");
            else
                log.info("缺省基础目录添加成功");
            return;
        }
        leafCategorySet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (int i = 0; i < categoryList.size(); ++i) {
            Category category = categoryList.get(i);
            if (category.getParent() != null)//groundCategory的parent为null
                setAsParent(category, category.getParentId());
        }
        for (int i = 0; i < categoryList.size(); ++i) {
            Category category = categoryList.get(i);
            if (category.getSubCategoryList().isEmpty())
                setLeaf(category, true);
            else//只有叶子节点的帖子计数值可以保证准确，因为校对时只校对叶子节点
                category.setIsLeaf(false).setOriPostCnt(0L);
            //设置路径
            setPathList(category);
        }
        //设置帖子数
        cascadeSetOriPostCnt(groundCategory);
        // 设置允许发帖管理等级
        groundCategory.setAllowPostLevel(groundCategory.getOriAllowPostLevel());
        cascadeSetAllowPostLevel(groundCategory);
        Globals.CategoryServiceInitialized = true;
        log.info("目录树加载完成");
    }

    /**
     * 级联地设置基础目录对象的帖子数据，在初始化和校正数据的时候调用
     * @param root
     * @return
     */
    private long cascadeSetOriPostCnt(@NotNull Category root) {
        if (root.getIsLeaf() == null || root.getIsLeaf())
            return root.getOriPostCnt();
        List<Category> subList = root.getSubCategoryList();
        for (int i = 0; i < subList.size(); ++i)
            root.setOriPostCnt(root.getOriPostCnt() + cascadeSetOriPostCnt(subList.get(i)));
        return root.getOriPostCnt();
    }

    /**
     * 级联地设置基础目录对象的允许发帖管理等级，在初始化和校正数据的时候调用
     * @param root
     * @return
     */
    private void cascadeSetAllowPostLevel(@NotNull Category root) {
        // 原则是子目录等级不能低于父目录，通过调用前判断以及递归操作保证
        Integer thisLevel = root.getOriAllowPostLevel();
        Integer parentLevel = root.getParent() == null ? null : root.getParent().getAllowPostLevel();
        // 因为是从根目录开始递归，所以若当前目录为空，则其所有父目录必为空，若不为空，则大于等于所有父目录等级
        if (parentLevel == null) {
            // 父目录等级为空，则设为该目录的原始等级
            root.setAllowPostLevel(thisLevel);
        } else if (thisLevel == null || thisLevel < parentLevel) {
            // 当前等级为空或者当前等级小于父目录等级，则直接设为父目录等级，从而使子目录等级必大于等于父目录
            root.setAllowPostLevel(parentLevel);
        } else {
            // 否则，设为当前原始等级
            root.setAllowPostLevel(thisLevel);
        }
        List<Category> subList = root.getSubCategoryList();
        if (subList != null) {
            for (int i = 0; i < subList.size(); ++i) {
                cascadeSetAllowPostLevel(subList.get(i));
            }
        }
    }

    /**
     * 级联获取允许发帖等级
     */
    private Integer cascadeGetAllowPostLevel(Category category) {
        if (GROUND_ID.equals(category.getId())) return category.getAllowPostLevel();
        Integer level = category.getAllowPostLevel();
        if (level != null) return level;
        return cascadeGetAllowPostLevel(category.getParent());
    }

    /**
     * 设置该目录的路径列表
     */
    @NotNull
    private void setPathList(Category category) {
        Category tmp = category;
        List<Category> pathList = new ArrayList<>();
        while (tmp != null) {
            Category path = new Category(tmp.getId()).setName(tmp.getName());
            path.setType(tmp.getType());
            pathList.add(path);
            tmp = tmp.getParent();
        }
        category.setPathList(pathList);
    }

    /**
     * 更新目录结构后，重置该目录及其所有子目录的目录路径
     */
    private void setPathListOfCascadeSub(Category category) {
        setPathList(category);
        List<Category> subs = category.getSubCategoryList();
        for (int i = 0; i < subs.size(); ++i)
            setPathListOfCascadeSub(subs.get(i));
    }

    /**
     * 深度优先搜索将该目录节点的所有级联的子节点都放到结果列表中
     */
    private void deepFirstSearchLeavesToList(@NotNull Category category, List<Category> resultList, boolean preventPrivate) {
        if (category.getIsLeaf() == null || category.getIsLeaf())
            resultList.add(category);
        List<Category> curSubCategories = category.getSubCategoryList();
        if (curSubCategories.size() == 0)
            return;
        for (int i = 0; i < curSubCategories.size(); ++i) {
            Category subCategory = curSubCategories.get(i);
            if (preventPrivate && subCategory.getType() != null && subCategory.getType().equals(Category.TYPE_PRIVATE)) {
                // preventPrivate为true，则忽略私有目录
                continue;
            }
            deepFirstSearchLeavesToList(subCategory, resultList, preventPrivate);
        }
    }

    /**
     * 深搜生成目录树的字符串
     */
    private void getSubTreeString(Category category, StringBuilder stringBuilder, int depth) {
        for (int i = 0; i < depth; ++i)
            stringBuilder.append("   ");
        String type = "";
        if (category.getType() == null || category.getType().equals(Category.TYPE_COMMON))
            type = "common";
        else if (category.getType().equals(Category.TYPE_DEF_SUB))
            type = "def_sub";
        else if (category.getType().equals(Category.TYPE_PRIVATE))
            type = "private";
        stringBuilder.append(String.format("|----id:%04d name:%s type:%s psCnt:%d\n", category.getId(), category.getName(), type, category.getPostCnt()));
        List<Category> subCategoryList = category.getSubCategoryList();
        for (int i = 0; i < subCategoryList.size(); ++i)
            getSubTreeString(subCategoryList.get(i), stringBuilder, depth + 1);
    }

    /**
     * 设置叶目录
     */
    private void setLeaf(Category category, boolean isLeaf) {
        if (isLeaf) {
            category.setIsLeaf(true);
            leafCategorySet.add(category);
        } else {
            category.setIsLeaf(false);
            leafCategorySet.remove(category);
        }
    }

    /**
     * 设置指定目录的父目录
     */
    @Nullable
    private Category setAsParent(Category category, Integer parentId) {
        Category parent = categoryMap.get(parentId);
        if (parent == null)
            return null;
        parent.getSubCategoryList().add(category);
        category.setParent(parent);
        //若父目录之前是叶目录，则添加节点后不再是叶目录
        if (parent.getIsLeaf() == null || parent.getIsLeaf())
            setLeaf(parent, false);
        return parent;
    }

    /**
     * 从父目录的子目录列表中移除该目录
     */
    private void removeFromParent(@NotNull Category category) {
        Category parent = category.getParent();
        List<Category> subCategoryList = parent.getSubCategoryList();
        for (int i = 0; i < subCategoryList.size(); ++i) {
            if (category.getId().equals(subCategoryList.get(i).getId())) {
                subCategoryList.remove(i);
                break;
            }
        }
        if (subCategoryList.size() == 0)//移除后原父目录变为叶目录
            setLeaf(parent, true);
        cascadeUpdatePostCnt(parent.getId(), category.getPostCnt() * -1);
    }

    /**
     * 获取一级目录集合
     */
    public Result<List<Category>> readRootCategoryList() {
        return new Result<List<Category>>(SUCCESS).setObject(groundCategory.getSubCategoryList());
    }

    /**
     * 获取叶目录集合
     */
    public Result<Set<Category>> readLeafCategorySet() {
        return new Result<Set<Category>>(SUCCESS).setObject(leafCategorySet);
    }

    /**
     * 获取base目录
     */
    public Result<Category> readGroundCategory() {
        return new Result<Category>(SUCCESS).setObject(groundCategory);
    }

    /**
     * 以字符串的形式返回目录树
     */
    public Result<String> readCategoryTreeString() {
        StringBuilder stringBuilder = new StringBuilder();
        getSubTreeString(groundCategory, stringBuilder, 0);
        return new Result<String>(SUCCESS).setObject(stringBuilder.toString());
    }

    /**
     * 获取包含所有目录的Map
     */
    public Result<Map<Integer, Category>> readCategoryMap() {
        return new Result<Map<Integer, Category>>(SUCCESS).setObject(categoryMap).setMessage("包含所有目录的Map");
    }

    /**
     * 获得指定目录的全部级联的子目录
     */
    public Result<List<Category>> readLeavesOfRoot(Integer categoryId, boolean preventPrivate, Operator operator) {
        Result<Category> result = readCategory(categoryId, false, operator);
        if (result.getState() == FAIL)
            return new Result<List<Category>>(result);
        List<Category> resultList = new ArrayList<>();
        deepFirstSearchLeavesToList(result.getObject(), resultList, preventPrivate);
        return new Result<List<Category>>(SUCCESS).setObject(resultList);
    }

    /**
     * 获得指定目录的直接子目录
     */
    public Result<List<Category>> readDirectSubCategories(Integer categoryId, Operator operator) {
        Result<Category> result = readCategory(categoryId, false, operator);
        if (result.getState() == FAIL)
            return new Result<List<Category>>(result);
        return new Result<List<Category>>(SUCCESS).setObject(result.getObject().getSubCategoryList());
    }

    public void cascadeUpdatePostCnt(Integer categoryId, Long delta) {
        //并不操作Category对象，不用同步
        Category category = categoryMap.get(categoryId);
        List<Integer> idList = new ArrayList<>();
        while (category != null) {
            idList.add(category.getId());
            category = category.getParent();
        }
        redisHelper.multiUpdateCounter(idList, "pst", delta);
    }

    /**
     * 检查按照category对象修改后，目录结构是否合法，操作过程中不会修改目录结构，type表示操作类型
     */
    private Result<Category> checkCategoryStructure(@NotNull Category category, int type) {
        Category paramCategory = category;
        if (type == UPDATE) {//对象是参数对象，根据Id获取完整对象
            Result<Category> categoryResult = readCategory(category.getId(), false, progOperator);
            if (categoryResult.getState() == FAIL)
                return categoryResult.setMessage("参数对象检查失败，id错误");
            category = categoryResult.getObject();
        }
        if (paramCategory.getParent() != null && paramCategory.getParent().getId() != null) {//包含父目录更新操作
            Result<Category> parentResult = readCategory(paramCategory.getParent().getId(), true, progOperator);
            if (parentResult.getState() == FAIL)
                return parentResult.setMessage("操作失败，父目录id错误");
            Category parent = parentResult.getObject();
            if (type == UPDATE) {
                // 更新父目录的情况，parent为要移向的父目录
                // parentId是要修改的父目录Id，category是完整的原目录
                if (parent.getId().equals(category.getId()))
                    return new Result<Category>(FAIL).setErrorCode(1010010203).setMessage("操作失败，子目录不能和父目录相同");
                // 更新父目录的情况，新的父目录和原目录的父目录相同
                if (parent.getId().equals(category.getParent().getId()))
                    return new Result<Category>(FAIL).setErrorCode(1010010204).setMessage("操作失败，新的父目录不能和原有的父目录相同");
                // 新的父目录是原目录的子目录
                if (category.isParentOf(parent))
                    return new Result<Category>(FAIL).setErrorCode(1010010205).setMessage("操作失败，新的父目录不能是原目录的子目录");
            }
            // 要移向或在此创建的父目录是叶子节点且有内容
            if (parent.getIsLeaf() && !parent.getPostCnt().equals(0L))
                return new Result<Category>(FAIL).setErrorCode(1010010206).setMessage("操作失败，新的父目录不能包含帖子");
        }
        return new Result<Category>(Result.SUCCESS);
    }

    private void setCounter(Category category) {
        Long[] v = redisHelper.getRedisCounterCnt(category.getId(), "pst");
        long cnt = (v != null && v[0] != null) ? v[0] : 0;
        category.setPostCnt(category.getOriPostCnt() + cnt);
    }

    private void multiSetCounter(List<Category> categoryList) {
        List<List<String>> vList = redisHelper.multiGetRedisCounterCnt(categoryList, "pst");
        for (int i = 0; i < categoryList.size(); ++i) {
            Category category = categoryList.get(i);
            if (category == null)
                continue;
            long cnt = vList.get(i).get(0) != null ? Long.valueOf(vList.get(i).get(0)) : 0;
            category.setPostCnt(category.getOriPostCnt() + cnt);
        }
    }

    /**获取目录，设置父目录的counter和子目录列表的counter*/
    public Result<Category> readCategoryWithAllCounters(Integer categoryId, Operator operator) {
        Result<Category> result = readCategory(categoryId, true, operator);
        if (result.getState() == FAIL)
            return result;
        List<Category> oriSubList = result.getObject().getSubCategoryList();
        List<Category> subListClone = new ArrayList<>(oriSubList.size());
        for (int i = 0; i < oriSubList.size(); ++i)
            subListClone.add(oriSubList.get(i).clone());
        result.getObject().setSubCategoryList(subListClone);
        if (result.getState() == SUCCESS)
            multiSetCounter(subListClone);
        return result;
    }

    private Result<Category> getCategory(Integer categoryId, Operator operator) {
        if (categoryId == null)
            return new Result<Category>(FAIL).setErrorCode(1010010201).setMessage("查询失败，id不能为空");
        Category resCategory = categoryMap.get(categoryId);
        if (resCategory == null)
            return new Result<Category>(FAIL).setErrorCode(1010010205).setMessage("查询失败，id错误");
        return new Result<Category>(SUCCESS).setObject(resCategory);
    }

    /**
     * 读取目录，从基础的目录对象克隆出来，保证基础的目录对象不被修改
     */
    public Result<Category> readCategory(Integer categoryId, boolean withCounter, Operator operator) {
        Result<Category> result = getCategory(categoryId, operator);
        if (result.getState() == SUCCESS) {
            result.setObject(result.getObject().clone());
            if (withCounter)
                setCounter(result.getObject());
        }
        return result;
    }

    /**
     * 给目标对象列表添加目录对象属性
     * @param entityList
     * @param getCategoryIdMethod
     * @param setCategoryMethod
     * @param operator
     * @param <T>
     * @return
     */
    @SneakyThrows
    public <T> Result<Object> multiSetCategory(List<T> entityList, Method getCategoryIdMethod, Method setCategoryMethod, Operator operator) {
        for (int i = 0; i < entityList.size(); ++i) {
            if (entityList.get(i) == null)
                continue;
            Category category = readCategory((Integer) getCategoryIdMethod.invoke(entityList.get(i)), false, operator).getObject();
            if (category != null)
                setCategoryMethod.invoke(entityList.get(i), category);
        }
        return new Result<Object>(Result.SUCCESS);
    }

    /**
     * 获取指定目录的所有父目录，包括base目录
     */
    public Result<List<Category>> readParentCategories(Integer categoryId, Operator operator) {
        Result<Category> result = readCategory(categoryId, false, operator);
        if (result.getState() == FAIL)
            return new Result<List<Category>>(result);
        Category category = result.getObject();
        List<Category> categoryList = new ArrayList<>();
        while (category.getParentId() != null) {
            category = category.getParent();
            categoryList.add(category);
        }
        return new Result<List<Category>>(SUCCESS).setObject(categoryList);
    }

    /**
     * 写入管理员操作记录
     * @param category
     * @param type
     * @param operator
     * @return
     */
    @SneakyThrows
    private Result<Integer> writeToAdminOpsRecord(Category category, int type, Operator operator) {
        AdminOpsRecord record = new AdminOpsRecord();
        record.setTargetId(category.getId()).setTarget(category).setDesc(objectMapper.writeValueAsString(category)).setOpComment(category.getOpComment()).setType(type);
        return adminOpsService.createAdminOpsRecord(record, operator);
    }

    /**
     * 添加目录，返回结果中包含新添加目录的id
     */
    public synchronized Result<Integer> createCategory(@NotNull Category category, Operator operator) {
        if (!category.isValidInsertParam(true) || !category.isValidOpComment())
            return new Result<Integer>(FAIL).setErrorCode(1010010207).setMessage("操作失败，参数不合法");
        if (!categoryValidator.checkCreatePermission(category, operator))
            return new Result<Integer>(FAIL).setErrorCode(1010010208).setMessage("操作失败，权限错误");
        //检查目录结构是否合法
        Result<Category> checkResult = checkCategoryStructure(category, CREATE);
        if (checkResult.getState() == FAIL)
            return new Result<Integer>(checkResult);
        //设置创建者及预缓存数量初始值
        category.setCreator(operator.getUser()).setCacheNum(20).setOriPostCnt(0L).setAllowUserPost(true);
        int res = insertOne(category);//数据库操作
        if (res == -1)//数据库操作失败
            return new Result<Integer>(FAIL).setErrorCode(1010010109).setMessage("数据库操作失败");
        //数据库操作成功后才修改内存数据
        //设置父目录
        setAsParent(category, category.getParentId());
        //新添加的目录一定是叶目录
        setLeaf(category, true);
        category.setPopularPostList(new IndexList(10));
        category.setLatestPostList(new IndexList(10));
        category.setAllowPostLevel(cascadeGetAllowPostLevel(category.getParent()));
        //添加到内存
        categoryMap.put(category.getId(), category);
        //设置路径
        setPathList(category);
        writeToAdminOpsRecord(category, AdminOpsRecord.TYPE_CREATE_CATEGORY, operator);
        return new Result<Integer>(SUCCESS).setObject(category.getId());
    }

    /**
     * 读取基础的目录对象，需谨慎调用，基础目录对象长期存在
     * @param categoryId
     * @param withCounter
     * @param operator
     * @return
     */
    private Result<Category> readOriginalCategory(Integer categoryId, boolean withCounter, Operator operator) {
        Result<Category> result = getCategory(categoryId, operator);
        if (result.getState() == SUCCESS) {
            if (withCounter)
                setCounter(result.getObject());
        }
        return result;
    }

    /**
     * 更换目录的父目录
     */
    public synchronized Result<Category> updateCategoryParent(@NotNull Category paramObject, Operator operator) {
        if (!paramObject.isValidUpdateParentParam() || !paramObject.isValidOpComment())
            return new Result<Category>(FAIL).setErrorCode(1010010210).setMessage("操作失败，参数不合法");
        if (!categoryValidator.checkUpdateParentPermission(paramObject, paramObject.getParentId(), operator))
            return new Result<Category>(FAIL).setErrorCode(1010010211).setMessage("操作失败，权限错误");
        Result<Category> categoryResult = readOriginalCategory(paramObject.getId(), true, operator);
        if (categoryResult.getState() == FAIL)//id错误
            return categoryResult;
        Result<Category> checkResult = checkCategoryStructure(paramObject, UPDATE);//检查目录结构是否合法
        if (checkResult.getState() == FAIL)//参数检查失败
            return checkResult;
        Category opParam = new Category(paramObject.getId()).setParentId(paramObject.getParentId());
        int res = updateOne(opParam);//数据库操作
        if (res == 0)//数据库操作失败
            return new Result<Category>(FAIL).setErrorCode(1010010112).setMessage("数据库操作失败");
        Category resCategory = categoryResult.getObject();
        Integer oriParentId = resCategory.getParentId();
        Integer newParentId = paramObject.getParentId();
        applyUpdateParentParam(resCategory, newParentId);
        postIndexService.transferCategoryIndexList(resCategory);
        // 处理新旧父目录的贴子数的变更
        cascadeUpdatePostCnt(oriParentId, resCategory.getPostCnt() * -1L);
        cascadeUpdatePostCnt(newParentId, resCategory.getPostCnt());
        opParam.setName(resCategory.getName()).setIconImg(resCategory.getIconImg());
        writeToAdminOpsRecord((Category) opParam.setOpComment(paramObject.getOpComment()), AdminOpsRecord.TYPE_UPDATE_CATEGORY_PARENT, operator);
        return categoryResult;
    }

    /**
     * 根据参数更新父目录
     */
    private void applyUpdateParentParam(Category category, @NotNull int newParentId) {
        removeFromParent(category);//从父目录中移除
        setAsParent(category, newParentId);//设置新的父目录
        cascadeUpdatePostCnt(newParentId, category.getPostCnt());
        setPathListOfCascadeSub(category);//重置该目录及其所有子目录的目录路径
    }

    /**
     * 更新目录信息
     */
    public synchronized Result<Category> updateCategoryInfo(@NotNull Category paramObject, Operator operator) {
        if (!paramObject.isValidUpdateInfoParam(true) || !paramObject.isValidOpComment())
            return new Result<Category>(FAIL).setErrorCode(1010010213).setMessage("操作失败，参数不合法");
        if (!categoryValidator.checkUpdateInfoPermission(paramObject, operator))
            return new Result<Category>(FAIL).setErrorCode(1010010214).setMessage("操作失败，权限错误");
        Result<Category> categoryResult = readOriginalCategory(paramObject.getId(), false, operator);
        if (categoryResult.getState() == FAIL) {
            // id错误
            return categoryResult;
        }
        Category opParam = new Category(paramObject.getId()).copyUpdateInfoParam(paramObject);
        int res = updateOne(opParam);
        if (res == 0)
            return new Result<Category>(FAIL).setErrorCode(1010010115).setMessage("数据库操作失败");
        // 数据库操作成功后才修改内存数据
        Category resCategory = categoryResult.getObject();
        resCategory.copyUpdateInfoParam(paramObject);
        if (paramObject.getName() != null) {
            // 更改名字，要级联更新该目录及子目录的路径信息
            setPathListOfCascadeSub(resCategory);
        }
        opParam.setName(resCategory.getName()).setIconImg(resCategory.getIconImg());
        writeToAdminOpsRecord((Category) opParam.setOpComment(paramObject.getOpComment()), AdminOpsRecord.TYPE_UPDATE_CATEGORY_INFO, operator);
        return categoryResult;
    }

    /**
     * 更新目录的展示顺序
     * @param paramObject
     * @param operator
     * @return
     */
    public synchronized Result<Category> updateCategoryDispSeq(@NotNull Category paramObject, Operator operator) {
        if (!paramObject.isValidUpdateDispSeqParam() || !paramObject.isValidOpComment())
            return new Result<Category>(FAIL).setErrorCode(1010010229).setMessage("操作失败，参数不合法");
        Result<Category> categoryResult = readOriginalCategory(paramObject.getId(), false, operator);
        if (categoryResult.getState() == FAIL)
            return categoryResult;
        Category resCategory = categoryResult.getObject();
        if (!categoryValidator.checkUpdateDispSeqPermission(resCategory, operator))
            return new Result<Category>(FAIL).setErrorCode(1010010230).setMessage("操作失败，权限错误");
        Category opParam = new Category(paramObject.getId()).setDispSeq(paramObject.getDispSeq());
        int res = updateOne(opParam);
        if (res == 0)
            return new Result<Category>(FAIL).setErrorCode(1010010131).setMessage("数据库操作失败");
        resCategory.setDispSeq(paramObject.getDispSeq());
        opParam.setName(resCategory.getName()).setIconImg(resCategory.getIconImg());
        writeToAdminOpsRecord((Category) opParam.setOpComment(paramObject.getOpComment()), AdminOpsRecord.TYPE_UPDATE_CATEGORY_DISP_SEQ, operator);
        return categoryResult;
    }

    /**
     * 更新目录的缓存数量
     * @param paramObject
     * @param operator
     * @return
     */
    public synchronized Result<Category> updateCategoryCacheNum(@NotNull Category paramObject, Operator operator) {
        if (!paramObject.isValidUpdateCacheNumParam() || !paramObject.isValidOpComment())
            return new Result<Category>(FAIL).setErrorCode(1010010232).setMessage("操作失败，参数不合法");
        Result<Category> categoryResult = readOriginalCategory(paramObject.getId(), false, operator);
        if (categoryResult.getState() == FAIL)
            return categoryResult;
        Category resCategory = categoryResult.getObject();
        if (!categoryValidator.checkUpdateCacheNumPermission(resCategory, operator))
            return new Result<Category>(FAIL).setErrorCode(1010010233).setMessage("操作失败，权限错误");
        Category opParam = new Category(paramObject.getId()).setCacheNum(paramObject.getCacheNum());
        int res = updateOne(opParam);
        if (res == 0)
            return new Result<Category>(FAIL).setErrorCode(1010010134).setMessage("数据库操作失败");
        resCategory.setCacheNum(paramObject.getCacheNum());
        opParam.setName(resCategory.getName()).setIconImg(resCategory.getIconImg());
        writeToAdminOpsRecord((Category) opParam.setOpComment(paramObject.getOpComment()), AdminOpsRecord.TYPE_UPDATE_CATEGORY_CACHE_NUM, operator);
        return categoryResult;
    }

    /**
     * 设置置顶帖
     */
    public synchronized Result<Category> setCategoryTopPost(@NotNull Category param, boolean isCancel, Operator operator) {
        if (!isCancel && !param.isValidSetTopPostParam())
            return new Result<Category>(FAIL).setErrorCode(1010010222).setMessage("操作失败，参数不合法");
        if (isCancel) {
            if (param.getId() == null)
                return new Result<Category>(FAIL).setErrorCode(1010010223).setMessage("操作失败，参数不合法");
            param.setTopPostId(null);
        }
        if (!param.isValidOpComment())
            return new Result<Category>(FAIL).setErrorCode(1010010235).setMessage("操作失败，参数不合法");
        Result<Category> categoryResult = readOriginalCategory(param.getId(), false, operator);
        if (categoryResult.getState() == FAIL)
            return categoryResult;
        if (!categoryValidator.checkUpdateTopPostPermission(param, operator))
            return new Result<Category>(FAIL).setErrorCode(1010010221).setMessage("操作失败，权限错误");
        Category resCategory = categoryResult.getObject();
        if (!isCancel) {
            Result<Post> postResult = postService.readPostWithCategoryInfo(param.getTopPostId(), operator);
            if (postResult.getState() == FAIL)
                return new Result<Category>(postResult);
            Post post = postResult.getObject();
            if (!resCategory.getId().equals(post.getCategoryId()) &&
                    !resCategory.isParentOf(post.getCategory()))
                return new Result<Category>(FAIL).setErrorCode(1010010220).setMessage("操作失败，置顶目标不在本目录");
        }
        int res = dao.updateTopPost(param.getId(), param.getTopPostId());
        if (res == 0)//数据库操作失败
            return new Result<Category>(FAIL).setErrorCode(1010010127).setMessage("数据库操作失败");
        resCategory.setTopPostId(param.getTopPostId());
        int type = isCancel ? AdminOpsRecord.TYPE_CANCEL_CATEGORY_TOP_POST : AdminOpsRecord.TYPE_SET_CATEGORY_TOP_POST;
        Category opParam = new Category(param.getId()).setName(resCategory.getName()).setIconImg(resCategory.getIconImg());
        writeToAdminOpsRecord((Category) opParam.setTopPostId(param.getTopPostId()).setOpComment(param.getOpComment()), type, operator);
        return categoryResult;
    }

    /**
     * 设置允许发帖管理等级
     */
    public synchronized Result<Category> setAllowPostLevel(@NotNull Category param, boolean isCancel, Operator operator) {
        if (!isCancel && !param.isValidSetAllowPostLevelParam())
            return new Result<Category>(FAIL).setErrorCode(1010010238).setMessage("操作失败，参数不合法");
        if (isCancel) {
            if (param.getId() == null)
                return new Result<Category>(FAIL).setErrorCode(1010010239).setMessage("操作失败，参数不合法");
            param.setAllowPostLevel(null);
        }
        if (!param.isValidOpComment())
            return new Result<Category>(FAIL).setErrorCode(1010010240).setMessage("操作失败，参数不合法");
        Result<Category> categoryResult = readOriginalCategory(param.getId(), false, operator);
        if (categoryResult.getState() == FAIL)
            return categoryResult;
        Category resCategory = categoryResult.getObject();
        Integer parentLevel = resCategory.getParent().getAllowPostLevel();
        if (parentLevel != null && parentLevel > param.getAllowPostLevel()) {
            return new Result<Category>(FAIL).setErrorCode(1010010241).setMessage("操作失败，子目录等级不能低于父目录");
        }
        if (!categoryValidator.checkUpdateAllowPostLevelPermission(resCategory, param, operator))
            return new Result<Category>(FAIL).setErrorCode(1010010242).setMessage("操作失败，权限错误");
        int res = dao.updateAllowPostLevel(param.getId(), param.getAllowPostLevel());
        if (res == 0)//数据库操作失败
            return new Result<Category>(FAIL).setErrorCode(1010010143).setMessage("数据库操作失败");
        resCategory.setOriAllowPostLevel(param.getAllowPostLevel()).setAllowPostLevel(param.getAllowPostLevel());
        // 级联更新子节点
        cascadeSetAllowPostLevel(resCategory);
        int type = isCancel ? AdminOpsRecord.TYPE_CANCEL_ALLOW_POST_LEVEL : AdminOpsRecord.TYPE_SET_ALLOW_POST_LEVEL;
        Category opParam = new Category(param.getId()).setName(resCategory.getName()).setIconImg(resCategory.getIconImg());
        writeToAdminOpsRecord((Category) opParam.setAllowPostLevel(param.getAllowPostLevel()).setOpComment(param.getOpComment()), type, operator);
        return categoryResult;
    }

    /**
     * 设置默认子目录
     */
    public synchronized Result<Category> setDefaultSubCategory(@NotNull Category param, boolean isCancel, Operator operator) {
        if (!isCancel && !param.isValidSetDefSubParam())
            return new Result<Category>(FAIL).setErrorCode(1010010224).setMessage("操作失败，参数不合法");
        if (isCancel) {
            if (param.getId() == null)
                return new Result<Category>(FAIL).setErrorCode(1010010225).setMessage("操作失败，参数不合法");
            param.setDefSubId(null);
        }
        if (!param.isValidOpComment())
            return new Result<Category>(FAIL).setErrorCode(1010010236).setMessage("操作失败，参数不合法");
        Result<Category> categoryResult = readOriginalCategory(param.getId(), false, operator);
        if (categoryResult.getState() == FAIL)
            return categoryResult;
        if (!categoryValidator.checkUpdateDefSubPermission(param, operator))
            return new Result<Category>(FAIL).setErrorCode(1010010221).setMessage("操作失败，权限错误");
        Category resCategory = categoryResult.getObject();
        if (!isCancel) {
            Result<Category> subCategoryResult = readOriginalCategory(param.getDefSubId(), false, operator);
            if (subCategoryResult.getState() == FAIL)
                return subCategoryResult;
            Category subCategory = subCategoryResult.getObject();
            if (subCategory.getParent() == null || !subCategory.getParent().getId().equals(resCategory.getId()))
                return new Result<Category>(FAIL).setErrorCode(1010010226).setMessage("默认子目录不在本目录");
        }
        int res = dao.updateDefSub(param.getId(), param.getDefSubId());//数据库操作
        if (res == 0)//数据库操作失败
            return new Result<Category>(FAIL).setErrorCode(1010010128).setMessage("数据库操作失败");
            resCategory.setDefSubId(param.getDefSubId());
        int type = isCancel ? AdminOpsRecord.TYPE_CANCEL_CATEGORY_DEF_SUB : AdminOpsRecord.TYPE_SET_CATEGORY_DEF_SUB;
        Category opParam = new Category(param.getId()).setName(resCategory.getName()).setIconImg(resCategory.getIconImg());
        writeToAdminOpsRecord((Category) opParam.setDefSubId(param.getDefSubId()).setOpComment(param.getOpComment()), type, operator);
        return categoryResult;
    }

    /**
     * 重新加载数据库目录信息，超级管理员操作
     */
    public synchronized Result<Object> reloadCategory(Operator operator) {
        if (!operator.isSuperAdmin()) return new Result<Object>(Result.FAIL);
        loadCategoryFromDB(true);
        return new Result<Object>(Result.SUCCESS);
    }

    /**
     * 删除目录
     */
    public synchronized Result<Object> deleteCategory(Category param, Operator operator, boolean forcibly) {
        if (param.getId() == null || !param.isValidOpComment())
            return new Result<Object>(FAIL).setErrorCode(1010010237).setMessage("操作失败，参数不合法");
        Result<Category> result = readOriginalCategory(param.getId(), true, operator);
        if (result.getState() == FAIL)
            return new Result<Object>(result);
        Category category = result.getObject();
        if (!categoryValidator.checkDeletePermission(category, operator, forcibly))
            return new Result<Object>(FAIL).setErrorCode(1010010216).setMessage("操作失败，权限错误");
        if (!forcibly) {
            if (category.getIsLeaf() != null && !category.getIsLeaf())//非强制且该目录有子目录，则删除失败
                return new Result<Object>(FAIL).setErrorCode(1010010217)
                        .setMessage("非强制模式下不能删除有子目录的目录");
            if (category.getPostCnt() != 0)//非强制且该目录有内容，则删除失败
                return new Result<Object>(FAIL).setErrorCode(1010010218)
                        .setMessage("非强制模式下不能删除有内容的目录");
        }
        int res = deleteOne(param.getId());//数据库操作
        if (res == 0)//数据库操作失败
            return new Result<Object>(FAIL).setErrorCode(1010010119).setMessage("数据库操作失败");
        //数据库操作成功后才修改内存数据
        categoryMap.remove(param.getId());//更新内存数据
        if (category.getIsLeaf() == null || category.getIsLeaf())
            leafCategorySet.remove(category);
        removeFromParent(category);
        writeToAdminOpsRecord((Category) category.setOpComment(param.getOpComment()), AdminOpsRecord.TYPE_DELETE_CATEGORY, operator);
        return new Result<Object>(Result.SUCCESS);
    }

    /**
     * 校正目录的多个计数属性
     * @return
     */
    public synchronized String correctCounters() {
        int r = dao.correctPostCnt(new ArrayList<>(leafCategorySet));
        if (r == 0)
            return "目录数据校正错误，请重新校正";
        redisTemplate.delete(RedisUtil.scanAllKeys(redisTemplate, redisHelper.getCounterKey("*")));
        List<Category> categoryList = dao.selectList(new Category());
        for (int i = 0; i < categoryList.size(); ++i) {
            Category tmp = categoryList.get(i);
            Category ori = categoryMap.get(tmp.getId());
            if (ori.getIsLeaf())//只设置叶子
                ori.setOriPostCnt(tmp.getOriPostCnt());
            else
                ori.setOriPostCnt(0L);
        }
        cascadeSetOriPostCnt(groundCategory);//设置帖子数
        return "目录的帖子数校正" + r + "条数据";
    }

    @Scheduled(cron = "0 00 4 * * ?")
    public synchronized void dailyFlushRedisToDB() {
        List<TmpEntry>[] cntLists = redisHelper.flushRedisCounter("pst");
        commonService.batchUpdate("lpt_category", "category_id", "category_post_cnt", CommonService.OPS_INCR_BY, cntLists[0]);
        for (int i = 0; i < cntLists[0].size(); ++i) {
            TmpEntry entry = cntLists[0].get(i);
            Category category = readOriginalCategory(entry.getId(), false, progOperator).getObject();
            if (category != null)
                category.setOriPostCnt(category.getOriPostCnt() + (int) entry.getValue());
        }
        log.info("目录的帖子数量写入数据库");
    }

}
