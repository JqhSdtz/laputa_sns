package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.TmpEntry;
import com.laputa.laputa_sns.common.TmpListEntry;
import com.laputa.laputa_sns.dao.CommonDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公共服务，主要是一些公共的数据库操作方法
 * @author JQH
 * @since 下午 12:34 20/03/16
 */

@Service
public class CommonService {

    public final static int OPS_COPY = 1;
    public final static int OPS_INCR_BY = 2;

    private final CommonDao dao;

    @Value("${batch-insert-num-limit}")//#一次最多insert10000条记录
    private int insertNumLimit;

    public CommonService(CommonDao dao) {
        this.dao = dao;
    }

    /**
     * 添加id字段是为了防止安全模式下不包含id字段无法批量更新
     */
    public void clearIndexedFlag(String tableName, String idFieldName, String pFiledName, String lFiledName) {
        if (pFiledName != null)
            dao.clearIndexedFlag(tableName, idFieldName, pFiledName);
        if (lFiledName != null)
            dao.clearIndexedFlag(tableName, idFieldName, lFiledName);
    }

    /**
     * 根据ID更新一个表的某个字段
     * @param tableName
     * @param idFieldName
     * @param filedName
     * @param id
     * @param value
     * @return
     */
    public int updateOne(String tableName, String idFieldName, String filedName, Integer id, Object value) {
        return dao.updateOne(tableName, idFieldName, filedName, id, value);
    }

    /**
     * 根据ID列表批量更新表的某个字段
     * @param tableName
     * @param idFieldName
     * @param valueFiledName
     * @param ops
     * @param list
     * @return
     */
    public int batchUpdate(String tableName, String idFieldName, String valueFiledName, int ops, @NotNull List<TmpEntry> list) {
        if (list.size() == 0)
            return 0;
        int cnt = 0;
        if (list.size() < insertNumLimit)
            cnt = dao.batchUpdate(list, tableName, idFieldName, valueFiledName, ops);
        else {
            for (int from = 0, to = insertNumLimit; from < list.size(); from += insertNumLimit, to += insertNumLimit)
                cnt += dao.batchUpdate(list.subList(from, to > list.size() ? list.size() : to), tableName, idFieldName, valueFiledName, ops);
        }
        return cnt;
    }

     /**
     * 根据ID列表批量更新表的多个字段，<b>该方法尚未测试</b>
     * @param tableName
     * @param idFieldName
     * @param valueFiledNames
     * @param ops
     * @param list
     * @return
     */
    public int batchUpdateMulti(String tableName, String idFieldName, List<String> valueFiledNames, int ops, @NotNull List<TmpListEntry> list) {
        if (list.size() == 0)
            return 0;
        int cnt = 0;
        if (list.size() < insertNumLimit)
            cnt = dao.batchUpdateMulti(list, tableName, idFieldName, valueFiledNames, ops);
        else {
            for (int from = 0, to = insertNumLimit; from < list.size(); from += insertNumLimit, to += insertNumLimit)
                cnt += dao.batchUpdateMulti(list.subList(from, to > list.size() ? list.size() : to), tableName, idFieldName, valueFiledNames, ops);
        }
        return cnt;
    }
}
