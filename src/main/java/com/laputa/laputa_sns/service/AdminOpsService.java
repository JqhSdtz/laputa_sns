package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.BaseService;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.dao.AdminOpsRecordDao;
import com.laputa.laputa_sns.model.entity.AdminOpsRecord;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.model.entity.User;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理员操作服务
 * @author JQH
 * @since 下午 3:32 20/04/24
 */

@Service
public class AdminOpsService extends BaseService<AdminOpsRecordDao, AdminOpsRecord> {

    private final UserService userService;

    public AdminOpsService(@Lazy UserService userService) {
        this.userService = userService;
    }

    /**
     * 创建一条管理员操作记录
     * @param param
     * @param operator
     * @return
     */
    public Result<Integer> createAdminOpsRecord(AdminOpsRecord param, Operator operator) {
        if (!param.isValidInsertParam())
            return new Result(Result.FAIL).setErrorCode(1010190201).setMessage("操作失败，参数错误");
        if (!operator.isAdmin())
            return new Result(Result.FAIL).setErrorCode(1010190202).setMessage("操作失败，权限错误");
        param.setCreator(operator.getUser());
        int res = insertOne(param);
        if (res == -1)
            return new Result(Result.FAIL).setErrorCode(1010190103).setMessage("数据库操作失败");
        return new Result(Result.SUCCESS).setObject(res);
    }

    /**
     * 读取一条管理员操作记录
     * @param id
     * @param operator
     * @return
     */
    public Result<AdminOpsRecord> readRecord(Integer id, Operator operator) {
        if (id == null)
            return new Result(Result.FAIL).setErrorCode(1010190204).setMessage("操作失败，参数错误");
        if (!operator.isAdmin())
            return new Result(Result.FAIL).setErrorCode(1010190205).setMessage("操作失败，权限错误");
        AdminOpsRecord res = selectOne(id);
        if (res == null)
            return new Result(Result.FAIL).setErrorCode(1010190106).setMessage("数据库操作失败");
        Result<User> creatorResult = userService.readUserWithAllFalse(res.getCreatorId(), operator);
        if (creatorResult.getState() == Result.SUCCESS)
            res.setCreator(creatorResult.getObject());
        return new Result(Result.SUCCESS).setObject(res);
    }

    /**
     * 根据条件读取多条管理员操作记录
     * @param param
     * @param operator
     * @return
     */
    @SneakyThrows
    public Result<List<AdminOpsRecord>> readRecordList(AdminOpsRecord param, Operator operator) {
        if (!param.isValidReadIndexParam())
            return new Result(Result.FAIL).setErrorCode(1010190207).setMessage("操作失败，参数错误");
        if (!operator.isAdmin())
            return new Result(Result.FAIL).setErrorCode(1010190208).setMessage("操作失败，权限错误");
        if (param.getQueryParam().getStartId().equals(0))
            param.getQueryParam().setStartId(Integer.MAX_VALUE);
        List<AdminOpsRecord> resList = selectList(param);
        if (resList == null)
            return new Result(Result.FAIL).setErrorCode(1010190109).setMessage("数据库操作失败");
        userService.multiSetUser(resList, AdminOpsRecord.class.getMethod("getCreatorId" ), AdminOpsRecord.class.getMethod("setCreator", User.class));
        return new Result(Result.SUCCESS).setObject(resList);
    }
}
