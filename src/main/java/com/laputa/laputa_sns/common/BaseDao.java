package com.laputa.laputa_sns.common;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JQH
 * @since 下午 7:06 20/02/05
 */

@Repository
public interface BaseDao<Type extends AbstractBaseEntity> {
    /**按ID获取一条数据*/
    Type selectOne(int id);

    /**按条件获取一条数据*/
    Type selectOne(Type entity);

    /**按条件查询多条数据*/
    List<Type> selectList(Type entity);

    /**根据多个id查询多条数据*/
    List<Type> selectListByMultiId(@Param("entity") Type entity, @Param("idList") List<Integer> idList);

    /**插入一条数据，成功则返回1*/
    int insertOne(Type entity);

    /**插入多条数据，返回插入成功的条数*/
    int insertList(List<Type> entityList);

    /**更新一条数据，成功则返回1*/
    int updateOne(Type entity);

    /**更新多条数据，返回更新成功的条数*/
    int updateList(List<Type> entityList);

    /**删除一条数据(逻辑删除)*/
    int deleteOne(int id);

    /**删除一条数据(逻辑删除)*/
    int deleteOne(Type entity);
}
