package com.laputa.laputa_sns.executor;

import com.laputa.laputa_sns.common.AbstractBaseEntity;
import com.laputa.laputa_sns.common.QueryParam;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.AbstractContent;
import com.laputa.laputa_sns.model.entity.Operator;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import java.util.ArrayList;
import java.util.List;

import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * @author JQH
 * @since 上午 10:35 20/04/10
 */

public class IndexExecutor<T extends AbstractBaseEntity> {

    public interface GetIdListCallBack<T extends AbstractBaseEntity> {
        /**
         * 要找的索引不存在则返回null，索引存在但位置不存在时应返回空的ArrayList
         */
        void getIdList(IndexExecutor executor);
    }

    public interface MultiReadEntityCallBack<T extends AbstractBaseEntity> {
        Result<List<T>> multiReadEntity(IndexExecutor executor);
    }

    public interface GetDBListCallBack<T extends AbstractBaseEntity> {
        Result<List<T>> getDBList(IndexExecutor executor) throws NoSuchMethodException;
    }

    public interface MultiSetRedisIndexCallBack<T extends AbstractBaseEntity> {
        void multiSetRedisIndex(List<T> entityList, IndexExecutor executor);
    }

    public interface ReadOneEntityCallBack<T extends AbstractBaseEntity> {
        Result<T> readOneEntity(Integer id, IndexExecutor executor);
    }

    public static class CallBacks<T extends AbstractBaseEntity> {
        public GetIdListCallBack<T> getIdListCallBack;
        public MultiReadEntityCallBack<T> multiReadEntityCallBack;
        public GetDBListCallBack<T> getDBListCallBack;
        public MultiSetRedisIndexCallBack<T> multiSetRedisIndexCallBack;
        public ReadOneEntityCallBack<T> readOneEntityCallBack;
    }

    public class Param<T extends AbstractBaseEntity> {
        public List<TypedTuple<String>> indexSetList;
        public T paramEntity, queryEntity;
        public Integer topId;
        public Integer type;
        public Long childNumOfParent;
        public Integer newStartId, newFrom;
        public String newStartValue;
        public int initFrom = 0;
        public int initStartId = 0;
        public List<T> entityList;
        public List<Integer> idList;
        public boolean disablePatchFromDB;
        public Operator operator;
        public Object addition;
    }

    public CallBacks<T> callBacks;

    public Param<T> param;

    public IndexExecutor(T paramEntity, T queryEntity, Integer type, CallBacks<T> callBacks, Operator operator) {
        this.param = new Param();
        this.callBacks = callBacks;
        this.param.paramEntity = paramEntity;
        this.param.queryEntity = queryEntity;
        this.param.type = type;
        this.param.operator = operator;
    }

    @SneakyThrows
    public Result<List<T>> doIndex() {
        if (param.childNumOfParent != null && (param.childNumOfParent == 0
                || param.childNumOfParent <= param.paramEntity.getQueryParam().getFrom()))
            return new Result(SUCCESS).setObject(new ArrayList(0));
        int queryNum = param.paramEntity.getQueryParam().getQueryNum();
        param.queryEntity.getQueryParam().setAddition(param.paramEntity.getQueryParam().getAddition());
        callBacks.getIdListCallBack.getIdList(this);
        QueryParam queryParam = param.paramEntity.getQueryParam();
        if (param.idList != null && param.idList.size() != 0) {
            Result<List<T>> entityListResult = callBacks.multiReadEntityCallBack.multiReadEntity(this);
            if (entityListResult.getState() == Result.FAIL)
                return entityListResult;
            param.entityList = entityListResult.getObject();
            if (!param.disablePatchFromDB && param.entityList.size() < queryNum) {
                Integer oriFrom = queryParam.getFrom();
                queryParam.setFrom(queryParam.getFrom() + param.entityList.size())
                        .setQueryNum(queryNum - param.entityList.size());
                Integer oriStartId = queryParam.getStartId();
                String oriStartValue = queryParam.getStartValue();
                if (param.newStartValue != null)
                    queryParam.setStartValue(param.newStartValue);
                if (param.newStartId != null) {
                    queryParam.setStartId(param.newStartId);
                } else if (param.newStartValue == null && param.entityList.size() != 0)
                    queryParam.setStartId(param.entityList.get(param.entityList.size() - 1).getId());
                if (param.childNumOfParent != null && queryParam.getFrom() < param.childNumOfParent) {
                    List<T> dbList = callBacks.getDBListCallBack.getDBList(this).getObject();
                    if (dbList != null) {
                        for (int i = 0; i < dbList.size(); ++i)
                            param.entityList.add(dbList.get(i));
                        if (dbList.size() != 0 && callBacks.multiSetRedisIndexCallBack != null)//Redis中没有，则放入Redis
                            callBacks.multiSetRedisIndexCallBack.multiSetRedisIndex(dbList, this);
                    }
                }
                queryParam.setFrom(oriFrom).setStartId(oriStartId).setStartValue(oriStartValue);
            }
        } else {
            if (param.idList == null)
                param.paramEntity.getQueryParam().setStartId(param.initStartId).setFrom(param.initFrom);
            Result<List<T>> entityListResult = callBacks.getDBListCallBack.getDBList(this);
            if (entityListResult.getState() == Result.FAIL)
                return entityListResult;
            param.entityList = entityListResult.getObject();
            if (param.entityList.size() != 0 && callBacks.multiSetRedisIndexCallBack != null)
                callBacks.multiSetRedisIndexCallBack.multiSetRedisIndex(param.entityList, this);
        }
        if (param.topId != null) {
            boolean set = queryParam.getStartId().equals(0) && queryParam.getFrom().equals(0);
            List<T> tmpList = new ArrayList(param.entityList.size() + 1);
            if (set)
                tmpList.add(null);
            T topEntity = null;
            for (int i = 0; i < param.entityList.size(); ++i)
                if (!param.topId.equals(param.entityList.get(i).getId()))
                    tmpList.add(param.entityList.get(i));
                else
                    topEntity = param.entityList.get(i);
            if (set) {
                if (topEntity == null)
                    topEntity = callBacks.readOneEntityCallBack.readOneEntity(param.topId, this).getObject();
                if (topEntity != null)
                    ((AbstractContent) topEntity).setIsTopped(true);
                tmpList.set(0, topEntity);
            }
            param.newFrom = queryParam.getFrom() + param.entityList.size();
            //topEntity另外读取的，是顺序之外的内容，不能影响正常顺序，所以设置考虑置顶之后的顺序前确定from的值
            param.entityList = tmpList;
        }
        param.queryEntity.getQueryParam().setStartId(param.newStartId).setStartValue(param.newStartValue).setFrom(param.newFrom);
        return new Result(Result.SUCCESS).setObject(param.entityList);
    }
}
