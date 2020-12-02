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
 * 索引执行对象，用于执行各种需要redis和数据库联合组成的索引中进行
 * 查找、增加和删除的逻辑
 *
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
        /**
         * 获取索引列表中每一项的ID，一般从redis中获取
         */
        public GetIdListCallBack<T> getIdListCallBack;
        /**
         * 根据索引列表中的ID获取索引列表中的完整对象
         */
        public MultiReadEntityCallBack<T> multiReadEntityCallBack;
        /**
         * 从数据库中加载索引列表，加载完整对象
         */
        public GetDBListCallBack<T> getDBListCallBack;
        /**
         * 将数据库中获取的索引列表添加到redis中
         */
        public MultiSetRedisIndexCallBack<T> multiSetRedisIndexCallBack;
        /**
         * 根据ID读取一个完整对象，一般用于读取置顶对象
         */
        public ReadOneEntityCallBack<T> readOneEntityCallBack;
    }

    public class Param<T extends AbstractBaseEntity> {
        /**
         * 索引列表，保存一个索引对象ID和索引排序值的二元组列表，具体用途不定
         */
        public List<TypedTuple<String>> indexSetList;
        /**
         * paramEntity是作为参数的实体，queryEntity是用来返回处理数据的实体
         */
        public T paramEntity, queryEntity;
        /**
         * 索引中置顶实体的ID
         */
        public Integer topId;
        /**
         * 索引类型，热门索引或最新索引
         */
        public Integer type;
        /**
         * 被索引对象的所有根据条件可获取的子对象的数量
         */
        public Long childNumOfParent;
        /**
         * newStartId是下一次查询的开始位置的对象ID，newFrom是下一次查询的开始位置
         */
        public Integer newStartId, newFrom;
        /**
         * 有些索引的对象是一个ID和其他数值的复合字符串，需要用startValue这个属性记录一下
         * newStartValue就是下一次查询用到的startValue
         */
        public String newStartValue;
        /**
         * 初始化查询的起始位置，即没有设定from值或from值无效时的索引开始位置
         */
        public int initFrom = 0;
        /**
         * 初始化查询的起始对象ID，同上
         */
        public int initStartId = 0;
        /**
         * 查询的索引列表的结果，包含完整实体对象
         */
        public List<T> entityList;
        /**
         * 索引列表对象的ID列表，一般用于去redis中查询索引对象列表
         */
        public List<Integer> idList;
        /**
         * 若从redis中查出来的索引列表长度不够，是否去数据库查询并补充到redis的索引列表中
         * 若该值为true，则会从数据库查询并补充，否则不会
         */
        public boolean disablePatchFromDB;
        /**
         * 查询的操作者
         */
        public Operator operator;
        /**
         * 查询的附加条件，用途不定
         */
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
                // 暂时改变原有参数，以便数据库查询
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
                // 恢复原有参数
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
            //topEntity另外读取的，是顺序之外的内容，不能影响正常顺序，所以设置考虑置顶之后的顺序前确定from的值
            param.newFrom = queryParam.getFrom() + param.entityList.size();
            param.entityList = tmpList;
        }
        param.queryEntity.getQueryParam().setStartId(param.newStartId).setStartValue(param.newStartValue).setFrom(param.newFrom);
        return new Result(Result.SUCCESS).setObject(param.entityList);
    }
}
