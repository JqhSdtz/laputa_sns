package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.Operator;
import com.laputa.laputa_sns.service.OSSService;
import com.laputa.laputa_sns.util.CryptUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author JQH
 * @since 下午 2:32 20/04/08
 */

@RestController
@RequestMapping(value = "/oss")
public class OSSController {
    private final OSSService ossService;

    public OSSController(OSSService ossService) {
        this.ossService = ossService;
    }

    @RequestMapping(value = "/{type}", method = RequestMethod.POST)
    public Result<String> upload(@NotNull MultipartFile file, @PathVariable String type, @RequestAttribute Operator operator) throws IOException {
        int bType;
        if ("ava".equals(type))
            bType = OSSService.AVATAR;
        else if ("pst".equals(type))
            bType = OSSService.POST;
        else if ("com".equals(type))
            bType = OSSService.COMMENT;
        else
            return Result.EMPTY_FAIL;
        return ossService.uploadImgSync(file.getBytes(), bType, operator);
    }
}
