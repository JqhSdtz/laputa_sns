package com.laputa.laputa_sns.controller;

import com.alibaba.druid.stat.DruidStatManagerFacade;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.CategoryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.laputa.laputa_sns.common.Result.SUCCESS;

/**
 * @author JQH
 * @since 上午 12:12 21/02/27
 */
@RestController
@RequestMapping(value = "/api/druid")
public class DruidStatController {
    @GetMapping("/stat")
    public Result druidStat(@RequestAttribute Operator operator){
        Map permissionMap = operator.getPermissionMap();
        Integer rootPermLevel  = permissionMap == null ? null : (Integer) permissionMap.get(CategoryService.GROUND_ID);
        if (rootPermLevel == null || rootPermLevel != 99)
            return Result.EMPTY_FAIL;
        // DruidStatManagerFacade#getDataSourceStatDataList 该方法可以获取所有数据源的监控数据，
        // 除此之外 DruidStatManagerFacade 还提供了一些其他方法，你可以按需选择使用。
        List<Map<String, Object>> dataList = DruidStatManagerFacade.getInstance().getDataSourceStatDataList();
        dataList.forEach(dataSource -> {
            Integer dataSourceId = (Integer) dataSource.get("Identity");
            List<Map<String, Object>> sqlStatList = DruidStatManagerFacade.getInstance().getSqlStatDataList(dataSourceId);
            dataSource.put("SqlStatList", sqlStatList);
        });
        return new Result(SUCCESS).setObject(dataList);
    }
}
