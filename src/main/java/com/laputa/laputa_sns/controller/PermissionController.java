package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.model.Permission;
import com.laputa.laputa_sns.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author JQH
 * @since 下午 4:02 20/02/13
 */

@Slf4j
@RestController
@RequestMapping(value = "/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result createPermission(@RequestBody Permission permission, @RequestAttribute Operator operator) {
        return permissionService.createPermission(permission, operator).setOperator(operator);
    }

    @RequestMapping(method = RequestMethod.PATCH)
    public Result updatePermission(@RequestBody Permission permission, @RequestAttribute Operator operator) {
        return permissionService.updatePermission(permission, operator).setOperator(operator);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result deletePermission(@RequestBody Permission permission, @RequestAttribute Operator operator) {
        return permissionService.deletePermission(permission, operator).setOperator(operator);
    }

    @RequestMapping(value = "/category/{categoryId}", method = RequestMethod.GET)
    public Result<List<Permission>> readPermissionOfCategory(@PathVariable Integer categoryId, @RequestAttribute Operator operator) {
        return permissionService.readPermissionList(new Permission().setOfType(Permission.OF_CATEGORY).setCategoryId(categoryId), operator).setOperator(operator);
    }

    @RequestMapping(value = "/user/{userId}", method = RequestMethod.GET)
    public Result<List<Permission>> readPermissionOfUser(@PathVariable Integer userId, @RequestAttribute Operator operator) {
        return permissionService.readPermissionList(new Permission().setOfType(Permission.OF_USER).setUserId(userId), operator).setOperator(operator);
    }

}
