package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.OSSService;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author JQH
 * @since 下午 2:32 20/04/08
 */

@RestController
@RequestMapping(value = "/api/oss")
public class OSSController {
    private final OSSService ossService;

    public OSSController(OSSService ossService) {
        this.ossService = ossService;
    }

    @RequestMapping(value = "/{type}", method = RequestMethod.POST)
    public Result<String> upload(@NotNull MultipartFile file, @PathVariable String type, @RequestAttribute Operator operator) throws IOException {
        int bType;
        if ("ava".equals(type)) {
            bType = OSSService.AVATAR;
        } else if ("cat".equals(type)) {
            bType = OSSService.CATEGORY;
        } else if ("pst".equals(type)) {
            bType = OSSService.POST;
        } else if ("com".equals(type)) {
            bType = OSSService.COMMENT;
        } else {
            return new Result<String>(Result.FAIL);
        }
        return ossService.uploadImgSync(file.getBytes(), bType, operator);
    }
}
