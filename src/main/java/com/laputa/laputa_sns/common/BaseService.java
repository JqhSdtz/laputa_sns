package com.laputa.laputa_sns.common;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * @author JQH
 * @since 下午 8:26 20/02/06
 */
public class BaseService<DaoType extends BaseDao<EntityType>, EntityType extends AbstractBaseEntity> {

    public static final int CREATE = 1;
    public static final int READ = 2;
    public static final int UPDATE = 3;
    public static final int DELETE = 4;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected DaoType dao;

    /**
     * 一次最多insert10000条记录
     */
    @Value("${batch-insert-num-limit}")
    private int insertNumLimit;

    /**
     * 按ID获取一条数据
     */
    public EntityType selectOne(int id) {
        return dao.selectOne(id);
    }

    /**
     * 按条件获取一条数据
     */
    public EntityType selectOne(EntityType entity) {
        return dao.selectOne(entity);
    }

    public List<EntityType> selectList(EntityType entity) {
        return dao.selectList(entity);
    }

    public List<EntityType> selectListByMultiId(EntityType entity, List<Integer> idList) {
        return dao.selectListByMultiId(entity, idList);
    }

    /**
     * 插入一条数据，成功则返回该条数据的Id，否则返回0
     */
    public int insertOne(EntityType entity) {
        int row = dao.insertOne(entity);
        return row == 0 ? -1 : entity.getId();
    }

    /**
     * 插入多条数据，返回插入成功的条数
     */
    public int insertList(@NotNull List<EntityType> entityList) {
        if (entityList.isEmpty()) {
            return 0;
        }
        int res = 0;
        if (entityList.size() < insertNumLimit) {
            res = dao.insertList(entityList);
        } else {
            for (int from = 0, to = insertNumLimit; from < entityList.size(); from += insertNumLimit, to += insertNumLimit) {
                res += dao.insertList(entityList.subList(from, to > entityList.size() ? entityList.size() : to));
            }
        }
        return res;
    }

    /**
     * 更新一条数据，成功返回1，否则返回0
     */
    public int updateOne(EntityType entity) {
        return dao.updateOne(entity);
    }

    /**
     * 更新多条数据，返回更新成功的条数
     */
    public int updateList(List<EntityType> entityList) {
        return dao.updateList(entityList);
    }

    /**
     * 删除一条数据(逻辑删除或物理删除)，成功返回1，否则返回0
     */
    public int deleteOne(int id) {
        return dao.deleteOne(id);
    }

    /**
     * 删除一条数据(逻辑删除或物理删除)，成功返回1，否则返回0
     */
    public int deleteOne(EntityType entity) {
        return dao.deleteOne(entity);
    }

}
