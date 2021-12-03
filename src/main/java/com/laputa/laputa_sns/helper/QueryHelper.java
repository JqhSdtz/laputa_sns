package com.laputa.laputa_sns.helper;

import com.laputa.laputa_sns.common.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.laputa.laputa_sns.common.Result.FAIL;
import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * @author JQH
 * @since 下午 7:18 20/03/13
 */

public class QueryHelper<T extends AbstractBaseEntity> {

    public interface SelectOneCallBack<T> {
        T selectOne(T param);
    }

    public interface SelectListCallBack<T> {
        List<T> selectList(T param);
    }

    public interface SelectListByMultiIdCallBack<T> {
        List<T> selectListByMultiId(T param, List<Integer> idList);
    }

    private final BaseService<?, T> service;
    private final RedisHelper<T> redisHelper;

    public SelectOneCallBack<T> selectOneCallBack;
    public SelectListCallBack<T> selectListCallBack;
    public SelectListByMultiIdCallBack<T> selectListByMultiIdCallBack;

    public QueryHelper(BaseService<?, T> service, RedisHelper<T> redisHelper) {
        this.service = service;
        this.redisHelper = redisHelper;
    }

    private T selectOne(T entity) {
        T res;
        if (selectOneCallBack != null)
            res = selectOneCallBack.selectOne(entity);
        else
            res = service.selectOne(entity);
        if (res == null) {
            return null;
        } else {
            res.setFromDB(true);
            return res;
        }
    }

    // private List<T> selectList(T entity) {
    //     List<T> resList;
    //     if (selectListCallBack != null)
    //         resList = selectListCallBack.selectList(entity);
    //     else
    //         resList = service.selectList(entity);
    //     if (resList != null)
    //         for (int i = 0; i < resList.size(); ++i)
    //             resList.get(i).setFromDB(true);
    //     return resList;
    // }

    private List<T> selectListByMultiId(T entity, List<Integer> idList) {
        List<T> resList;
        if (selectListByMultiIdCallBack != null)
            resList = selectListByMultiIdCallBack.selectListByMultiId(entity, idList);
        else
            resList = service.selectListByMultiId(entity, idList);
        if (resList != null)
            for (int i = 0; i < resList.size(); ++i)
                resList.get(i).setFromDB(true);
        return resList;
    }

    public boolean existEntity(@NotNull T entity) {
        if (redisHelper.existEntity(entity.getId()))
            return true;
        entity.setQueryParam(new QueryParam().setQueryType(QueryParam.EXIST));
        return selectOne(entity) != null;
    }

    public Result<T> readDBEntity(T entity) {
        T resEntity = selectOne(entity);
        if (resEntity == null)
            return new Result<T>(FAIL).setErrorCode(1010110101).setMessage(entity.getClass().getSimpleName() + "对象" + entity.getId() + "不存在");
        return new Result<T>(SUCCESS).setObject(resEntity);
    }

    public Result<T> readEntity(@NotNull T entity, boolean isFull) {
        T resEntity = redisHelper.getEntity(entity.getId(), isFull, false);
        if (resEntity == null) {
            if (isFull)
                entity.setQueryParam(new QueryParam().setQueryType(QueryParam.FULL));
            Result<T> entityResult = readDBEntity(entity);
            if (entityResult.getState() == FAIL)
                return entityResult;
            resEntity = entityResult.getObject();
            redisHelper.setEntity(resEntity, isFull);
        }
        return new Result<T>(SUCCESS).setObject(resEntity);
    }

    public Result<List<T>> multiReadEntity(@NotNull List<Integer> idList, boolean isFull, T queryEntity) {
        if (idList.size() == 0)
            return new Result<List<T>>(SUCCESS).setObject(new ArrayList<>(0));
        List<T> entityList = redisHelper.multiGetEntity(idList, isFull);
        List<Integer> dbIdList = new ArrayList<>();
        for (int i = 0; i < entityList.size(); ++i)
            if (entityList.get(i) == null)
                dbIdList.add(idList.get(i));
        if (dbIdList.size() == 0) // 从Redis中获取的对象都不为空
            return new Result<List<T>>(SUCCESS).setObject(entityList);
        if (isFull && queryEntity != null)
            queryEntity.setQueryParam(new QueryParam().setQueryType(QueryParam.FULL));
        List<T> dbEntityList = selectListByMultiId(queryEntity, dbIdList);
        if (dbEntityList == null)
            return new Result<List<T>>(Result.FAIL).setErrorCode(1010110103).setMessage("数据库操作失败");
        HashMap<Integer, T> dbEntityMap = new HashMap<>(dbEntityList.size());
        for (int i = 0; i < dbEntityList.size(); ++i)
            dbEntityMap.put(dbEntityList.get(i).getId(), dbEntityList.get(i));
        for (int i = 0; i < idList.size(); ++i)
            if (entityList.get(i) == null && dbEntityMap.containsKey(idList.get(i)))
                entityList.set(i, dbEntityMap.get(idList.get(i)));
        redisHelper.multiSetAndRefreshEntity(entityList, dbEntityMap, isFull);
        return new Result<List<T>>(SUCCESS).setObject(entityList);
    }
}
