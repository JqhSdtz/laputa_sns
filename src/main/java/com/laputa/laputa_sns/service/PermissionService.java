package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.common.BaseService;
import com.laputa.laputa_sns.common.QueryParam;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.dao.PermissionDao;
import com.laputa.laputa_sns.model.entity.*;
import com.laputa.laputa_sns.util.ProgOperatorManager;
import com.laputa.laputa_sns.validator.PermissionValidator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * 权限服务
 * @author JQH
 * @since 下午 11:11 20/02/12
 */

@Slf4j
@Service
public class PermissionService extends BaseService<PermissionDao, Permission> {

    private final CategoryService categoryService;
    private final UserService userService;
    private final AdminOpsService adminOpsService;
    private final PermissionValidator permissionValidator;
    private final OperatorService operatorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Operator progOperator = ProgOperatorManager.register(PermissionService.class);

    public PermissionService(@Lazy CategoryService categoryService, @Lazy UserService userService, AdminOpsService adminOpsService, @Lazy PermissionValidator permissionValidator, @Lazy OperatorService operatorService) {
        this.categoryService = categoryService;
        this.userService = userService;
        this.adminOpsService = adminOpsService;
        this.permissionValidator = permissionValidator;
        this.operatorService = operatorService;
        this.objectMapper.setFilterProvider(new SimpleFilterProvider()
                .addFilter("PermissionFilter", SimpleBeanPropertyFilter.filterOutAllExcept("type", "user", "category_id", "category_path", "level"))
                .addFilter("UserFilter", SimpleBeanPropertyFilter.filterOutAllExcept("nick_name", "raw_avatar", "type", "state")));
    }

    /**
     * 更新Redis中的该用户的权限列表
     */
    @Nullable
    private Map<Integer, Integer> updateRedisPermissionMap(Integer userId, Permission permission) {
        Operator operator = operatorService.getOnlineOperator(userId);
        if (operator != null) {
            Map<Integer, Integer> permissionMap = operator.getPermissionMap();
            if (permissionMap == null) {
                permissionMap = new HashMap<>();
                operator.setPermissionMap(permissionMap);
            }
            permissionMap.put(permission.getCategoryId(), permission.getLevel());
            operatorService.updateOnlineOperator(operator);
            return permissionMap;
        }
        return null;
    }

    /**
     * 删除Redis中指定用户的指定目录的权限
     */
    @Nullable
    private Map<Integer, Integer> removeRedisPermission(Integer userId, Integer categoryId) {
        Operator operator = operatorService.getOnlineOperator(userId);
        if (operator != null) {
            Map<Integer, Integer> permissionMap = operator.getPermissionMap();
            permissionMap.remove(categoryId);
            operatorService.updateOnlineOperator(operator);
            return permissionMap;
        }
        return null;
    }

    /**
     * 将权限列表转化为目录和权限的映射
     */
    @NotNull
    public Map<Integer, Integer> convertToPermissionMap(@NotNull List<Permission> permissionList) {
        Map<Integer, Integer> permissionMap = new HashMap();
        for (int i = 0; i < permissionList.size(); ++i) {
            Permission permission = permissionList.get(i);
            permissionMap.put(permission.getCategoryId(), permission.getLevel());
        }
        return permissionMap;
    }

    /**
     * 获取操作者在指定目录的权限等级，该方法会同时查找父目录的权限
     */
    public Result<Integer> readPermissionLevel(Integer categoryId, @NotNull Operator operator) {
        Map<Integer, Integer> permissionMap = operator.getPermissionMap();
        if (permissionMap == null || permissionMap.size() == 0)
            return Result.EMPTY_FAIL;
        if (permissionMap.containsKey(categoryId)) //操作者有该目录权限
            return new Result(SUCCESS).setObject(permissionMap.get(categoryId));
        //操作者没有该目录权限，检查是否有其父目录的权限
        Result<List<Category>> result = categoryService.readParentCategories(categoryId, operator);
        if (result.getState() == Result.FAIL) {//没有该目录
            Integer groundLevel = permissionMap.get(CategoryService.GROUND_ID);
            return groundLevel != null ? new Result(SUCCESS).setObject(groundLevel) : (Result<Integer>) (Result) result;
        }
        List<Category> categoryList = result.getObject();
        for (int i = 0; i < categoryList.size(); ++i)
            if (permissionMap.containsKey(categoryList.get(i).getId()))
                return new Result(SUCCESS).setObject(permissionMap.get(categoryList.get(i).getId()));
        return Result.EMPTY_FAIL;
    }

    /**
     * 从数据库读取一个完整的权限对象
     */
    private Result<Permission> readPermission(@NotNull Permission permission, Operator operator) {
        if (!permissionValidator.checkReadPermission(permission, operator))
            return new Result(FAIL).setErrorCode(1010030209).setMessage("操作失败，权限错误");
        if (permission.getId() == null && (permission.getCategoryId() == null || permission.getUserId() == null))
            return new Result(FAIL).setErrorCode(1010030210).setMessage("操作失败，参数错误");
        Permission resPermission = selectOne(permission);
        if (resPermission == null)
            return new Result(FAIL).setErrorCode(1010030106).setMessage("数据库操作失败");
        return new Result(SUCCESS).setObject(resPermission);
    }

    /**
     * 从数据库读取某个用户的权限表
     */
    public Result<Map<Integer, Integer>> readPermissionMapOfUser(@NotNull Integer userId, Operator operator) {
        if (userId == null)
            return new Result(FAIL).setErrorCode(1010030216).setMessage("操作错误，参数不合法");
        if (!permissionValidator.checkReadOfUserPermission(operator))
            return new Result(FAIL).setErrorCode(1010030212).setMessage("操作失败，权限错误");
        List<Permission> permissionList = selectList(new Permission().setUserId(userId));
        if (permissionList == null)
            return new Result(FAIL).setErrorCode(1010030105).setMessage("数据库操作失败");
        Map<Integer, Integer> permissionMap = convertToPermissionMap(permissionList);
        return new Result(SUCCESS).setObject(permissionMap);
    }

    @SneakyThrows
    public Result<List<Permission>> readPermissionList(Permission param, Operator operator) {
        if (param.getOfType() == null || (param.getOfType() == Permission.OF_CATEGORY && param.getCategoryId() == null)
                || (param.getOfType() == Permission.OF_USER && param.getUserId() == null))
            return new Result(FAIL).setErrorCode(1010030211).setMessage("操作错误，参数不合法");
        if (!operator.isAdmin())//只有管理员能查看目录的管理员列表和用户的管理员等级
            return new Result(FAIL).setErrorCode(1010030214).setMessage("操作失败，权限错误");
        List<Permission> permissionList = selectList((Permission) param.setQueryParam(new QueryParam().setQueryType(QueryParam.FULL)));
        if (permissionList == null)
            return new Result(FAIL).setErrorCode(1010030115).setMessage("数据库操作失败");
        if (param.getOfType() == Permission.OF_CATEGORY)
            userService.multiSetUser(permissionList, Permission.class.getMethod("getUserId"), Permission.class.getMethod("setUser", User.class));
        else if (param.getOfType() == Permission.OF_USER)
            categoryService.multiSetCategory(permissionList, Permission.class.getMethod("getCategoryId"), Permission.class.getMethod("setCategory", Category.class), operator);
        return new Result(SUCCESS).setObject(permissionList);
    }

    /**
     * 创建权限
     */
    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRED)
    public Result createPermission(@NotNull Permission permission, Operator operator) {
        if (!permission.isValidInsertParam() || !permission.isValidOpComment())
            return new Result(FAIL).setErrorCode(1010030201).setMessage("操作错误，参数不合法");
        if (!permissionValidator.checkCreatePermission(permission, operator))
            return new Result(FAIL).setErrorCode(1010030208).setMessage("操作失败，权限错误");
        Result<Permission> checkExistResult = readPermission(permission, operator);
        if (checkExistResult.getState() == SUCCESS)
            return new Result(FAIL).setErrorCode(1010030213).setMessage("该用户已经存在权限");
        Result<User> userResult = userService.readUserWithAllFalse(permission.getUserId(), operator);
        if (userResult.getState() == FAIL)
            return userResult;
        permission.setUser(userResult.getObject());
        permission.setCategory(categoryService.readCategory(permission.getCategoryId(),false, operator).getObject());
        permission.setCreator(operator.getUser());//设置创建者
        int res = insertOne(permission);
        if (res == -1)
            return new Result(FAIL).setErrorCode(1010030102).setMessage("数据库操作失败");
        Result redefineResult = userService.redefineUserState(permission.getUserId(), progOperator);
        if (redefineResult.getState() == FAIL) {//更新用户状态失败，回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return redefineResult;
        }
        updateRedisPermissionMap(permission.getUserId(), permission);
        AdminOpsRecord record = new AdminOpsRecord();
        record.setTargetId(permission.getUserId()).setDesc(objectMapper.writeValueAsString(permission)).setOpComment(permission.getOpComment()).setType(AdminOpsRecord.TYPE_CREATE_PERMISSION);
        return adminOpsService.createAdminOpsRecord(record, operator);
    }

    /**
     * 更新权限
     */
    @SneakyThrows
    public Result updatePermission(@NotNull Permission newPerm, Operator operator) {
        if (!newPerm.isValidUpdateParam() || !newPerm.isValidOpComment())
            return new Result(FAIL).setErrorCode(1010030202).setMessage("操作错误，参数不合法");
        Result<Permission> oldResult = readPermission(newPerm, operator);
        if (oldResult.getState() == FAIL)
            return oldResult;
        Permission oldPerm = oldResult.getObject();//更新权限前必须获取原有权限以比较是否有更新的权限
        if (!permissionValidator.checkUpdatePermission(oldPerm, newPerm, operator))
            return new Result(FAIL).setErrorCode(1010030207).setMessage("操作失败，权限错误");
        if (updateOne(newPerm) == 0)
            return new Result(FAIL).setErrorCode(1010030103).setMessage("数据库操作失败");
        newPerm.setUser(userService.readUserWithAllFalse(newPerm.getUserId(), operator).getObject());
        newPerm.setCategory(categoryService.readCategory(newPerm.getCategoryId(), false, operator).getObject());
        updateRedisPermissionMap(newPerm.getUserId(), newPerm);
        AdminOpsRecord record = new AdminOpsRecord();
        record.setTargetId(newPerm.getUserId()).setDesc(objectMapper.writeValueAsString(newPerm)).setOpComment(newPerm.getOpComment()).setType(AdminOpsRecord.TYPE_UPDATE_PERMISSION);
        return adminOpsService.createAdminOpsRecord(record, operator);
    }

    /**
     * 删除权限
     */
    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRED)
    public Result deletePermission(@NotNull Permission permission, Operator operator) {
        if (!permission.isValidDeleteParam() || !permission.isValidOpComment())
            return new Result(FAIL).setErrorCode(1010030203).setMessage("操作错误，参数不合法");
        Result<Permission> oriResult = readPermission(permission, operator);
        if (oriResult.getState() == FAIL)
            return oriResult;
        permission = oriResult.getObject();//要判断是否有删除权限，必须和要删除的权限比较
        if (!permissionValidator.checkDeletePermission(permission, operator))
            return new Result(FAIL).setErrorCode(1010030206).setMessage("操作失败，权限错误");
        if (deleteOne(permission) == 0)
            return new Result(FAIL).setErrorCode(1010030104).setMessage("数据库操作失败");
        permission.setUser(userService.readUserWithAllFalse(permission.getUserId(), operator).getObject());
        permission.setCategory(categoryService.readCategory(permission.getCategoryId(), false, operator).getObject());
        Result redefineResult = userService.redefineUserState(permission.getUserId(), progOperator);
        if (redefineResult.getState() == FAIL) {//更新用户状态失败，回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return redefineResult;
        }
        removeRedisPermission(permission.getUserId(), permission.getCategoryId());
        AdminOpsRecord record = new AdminOpsRecord();
        record.setTargetId(permission.getUserId()).setDesc(objectMapper.writeValueAsString(permission)).setOpComment(permission.getOpComment()).setType(AdminOpsRecord.TYPE_DELETE_PERMISSION);
        return adminOpsService.createAdminOpsRecord(record, operator);
    }

}
