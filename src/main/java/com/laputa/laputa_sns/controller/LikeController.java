package com.laputa.laputa_sns.controller;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.LikeRecord;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.LikeRecordService;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * @author JQH
 * @since 下午 2:45 20/02/25
 */

@RestController
@RequestMapping(value = "/api/like")
public class LikeController {

    private final LikeRecordService likeRecordService;
    private final HashMap<String, Integer> typeMap;

    public LikeController(LikeRecordService likeRecordService) {
        this.likeRecordService = likeRecordService;
        this.typeMap = new HashMap(3);
        this.typeMap.put("post", LikeRecord.TYPE_POST);
        this.typeMap.put("cml1", LikeRecord.TYPE_CML1);
        this.typeMap.put("cml2", LikeRecord.TYPE_CML2);
    }

    @RequestMapping(value = "/{type}", method = RequestMethod.POST)
    public Result createLikeRecord(@NotNull @RequestBody LikeRecord likeRecord, @PathVariable String type, @RequestAttribute Operator operator) {
        likeRecord.setType(typeMap.get(type));
        return likeRecordService.createLikeRecord(likeRecord, operator).setOperator(operator);
    }

    @RequestMapping(value = "/{type}", method = RequestMethod.DELETE)
    public Result deleteLikeRecord(@NotNull @RequestBody LikeRecord likeRecord, @PathVariable String type, @RequestAttribute Operator operator) {
        likeRecord.setType(typeMap.get(type));
        return likeRecordService.deleteLikeRecord(likeRecord, operator).setOperator(operator);
    }

    @RequestMapping(value = "/list/{type}", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<LikeRecord>> readTargetLikeList(@NotNull @RequestBody LikeRecord likeRecord, @PathVariable String type, @RequestAttribute Operator operator) {
        likeRecord.setType(typeMap.get(type));
        return likeRecordService.readTargetLikeList(likeRecord, operator).setOperator(operator);
    }

}
