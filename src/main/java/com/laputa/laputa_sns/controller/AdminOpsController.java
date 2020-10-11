package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.AdminOpsRecord;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.AdminOpsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author JQH
 * @since 下午 1:12 20/04/25
 */

@RestController
@RequestMapping(value = "/admin_ops")
public class AdminOpsController {

    private final AdminOpsService adminOpsService;

    public AdminOpsController(AdminOpsService adminOpsService) {
        this.adminOpsService = adminOpsService;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Result<AdminOpsRecord> readRecord(@PathVariable Integer id, @RequestAttribute Operator operator) {
        return adminOpsService.readRecord(id, operator).setOperator(operator);
    }

    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<AdminOpsRecord>> readRecordList(@RequestBody AdminOpsRecord param, @RequestAttribute Operator operator) {
        return adminOpsService.readRecordList(param, operator).setOperator(operator);
    }
}
