package com.laputa.laputa_sns.dao;

import com.laputa.laputa_sns.common.TmpEntry;
import com.laputa.laputa_sns.common.TmpListEntry;

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

    int batchUpdateMulti(@Param("list") List<TmpListEntry> list, @Param("tableName") String tableName, @Param("idFieldName") String idFieldName, @Param("valueFiledNames") List<String> valueFiledNames, @Param("ops") int ops);
}
