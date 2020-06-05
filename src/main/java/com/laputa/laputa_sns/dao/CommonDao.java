package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.common.TmpEntry;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JQH
 * @since 上午 11:44 20/03/16
 */

@Repository
public interface CommonDao {

    int clearIndexedFlag(@Param("tableName") String tableName, @Param("idFieldName") String idFieldName, @Param("indexedFlagFieldName") String indexedFlagFieldName);

    int updateOne(@Param("tableName") String tableName, @Param("idFieldName") String idFieldName, @Param("filedName") String filedName, @Param("id") Integer id, @Param("value") Object value);

    int batchUpdate(@Param("list") List<TmpEntry> list, @Param("tableName") String tableName, @Param("idFieldName") String idFieldName, @Param("valueFiledName") String valueFiledName, @Param("ops") int ops);
}
