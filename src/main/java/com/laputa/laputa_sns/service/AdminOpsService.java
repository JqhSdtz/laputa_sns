package com.laputa.laputa_sns.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.common.BaseService;
import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.dao.AdminOpsRecordDao;
import com.laputa.laputa_sns.model.entity.*;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员操作服务
 *
 * @author JQH
 * @since 下午 3:32 20/04/24
 */

@Service
public class AdminOpsService extends BaseService<AdminOpsRecordDao, AdminOpsRecord> {

    private final UserService userService;
    private final PostService postService;
    private final ObjectMapper opsRecordMapper;

    @Value("${admin-ops-record-category}")
    private int adminOpsRecordCategoryId;

    @Value("${admin-ops-record-user}")
    private int adminOpsRecordUserId;

    public AdminOpsService(@Lazy UserService userService, @Lazy PostService postService) {
        this.userService = userService;
        this.postService = postService;
        this.opsRecordMapper = new ObjectMapper();
        this.opsRecordMapper.setFilterProvider(new SimpleFilterProvider().addFilter("UserFilter", SimpleBeanPropertyFilter
                .filterOutAllExcept("id", "nick_name", "raw_avatar", "type", "state", "talk_ban_to", "top_post_id", "followers_cnt", "following_cnt", "post_cnt")));
    }

    public void processPost(Post post) {
//        String content = post.getContent();
//        if (content == null || content.length() < 18)
//            return;
//        String head = content.substring(0, 18);
//        String body = content.substring(18);
//        if ((getHead()).equals(head)) {
//            String newHead = "tp:amOps;ck:1;";
//            post.setContent(newHead + body);
//        }
    }

    private String getHead() {
        return "tp:amOps";
    }

    /**
     * 创建一条管理员操作记录
     *
     * @param param
     * @param operator
     * @return
     */
    public Result<Integer> createAdminOpsRecord(AdminOpsRecord param, Operator operator) {
        if (!param.isValidInsertParam())
            return new Result(Result.FAIL).setErrorCode(1010190201).setMessage("操作失败，参数错误");
        if (!operator.isAdmin())
            return new Result(Result.FAIL).setErrorCode(1010190202).setMessage("操作失败，权限错误");
        Post recordPost = (Post) new Post().setCategoryId(adminOpsRecordCategoryId).setCreatorId(adminOpsRecordUserId)
                .setType(Post.TYPE_PUBLIC);
        String recordContent = getHead() + "#";
        recordContent += "操作类型:" + param.getTypeStr() + "\n";
        switch (param.getType()) {
            case AdminOpsRecord.TYPE_DELETE_POST:
                Post post = ((Post) param.getTarget());
                String str = post.getTitle();
                if (str == null)
                    str = post.getContent();
                recordContent += "相关帖子:\"" + str + "\"";
                break;
            case AdminOpsRecord.TYPE_DELETE_CML1:
                recordContent += "相关评论:\"" + ((CommentL1) param.getTarget()).getContent() + "\"";
                break;
            case AdminOpsRecord.TYPE_DELETE_CML2:
                recordContent += "相关回复:\"" + ((CommentL2) param.getTarget()).getContent() + "\"";
                break;
            case AdminOpsRecord.TYPE_CREATE_PERMISSION:
            case AdminOpsRecord.TYPE_UPDATE_PERMISSION:
            case AdminOpsRecord.TYPE_DELETE_PERMISSION:
                recordContent += "相关评论:\"" + ((Permission) param.getTarget()).getUser().getNickName() + "\"";
                break;
            case AdminOpsRecord.TYPE_CREATE_CATEGORY:
            case AdminOpsRecord.TYPE_CANCEL_CATEGORY_DEF_SUB:
            case AdminOpsRecord.TYPE_SET_CATEGORY_DEF_SUB:
            case AdminOpsRecord.TYPE_SET_CATEGORY_TOP_POST:
            case AdminOpsRecord.TYPE_UPDATE_CATEGORY_CACHE_NUM:
            case AdminOpsRecord.TYPE_UPDATE_CATEGORY_DISP_SEQ:
            case AdminOpsRecord.TYPE_UPDATE_CATEGORY_PARENT:
                recordContent += "相关目录:\"" + ((Category) param.getTarget()).getName() + "\"";
                break;
        }
        recordContent += "\n操作理由:" + param.getOpComment();
        recordPost.setContent(recordContent);
        Map opRecordObj = new HashMap();
        opRecordObj.put("desc", param.getDesc());
        opRecordObj.put("comment", param.getOpComment());
        opRecordObj.put("type", param.getType());
        opRecordObj.put("type_str", param.getTypeStr());
        opRecordObj.put("creator", operator.getUser());
        try {
            recordPost.setFullText(this.opsRecordMapper.writeValueAsString(opRecordObj));
        } catch (JsonProcessingException e) {
            return new Result(Result.FAIL).setErrorCode(1010190110).setMessage("操作格式转换失败;" + e.getMessage());
        }
        param.setCreator(operator.getUser());
        int res = insertOne(param);
        if (res == -1)
            return new Result(Result.FAIL).setErrorCode(1010190103).setMessage("数据库操作失败");
        return postService.createPost(recordPost, new Operator(adminOpsRecordUserId));
    }

    /**
     * 读取一条管理员操作记录
     *
     * @param id
     * @param operator
     * @return
     */
    public Result<AdminOpsRecord> readRecord(Integer id, Operator operator) {
        if (id == null)
            return new Result(Result.FAIL).setErrorCode(1010190204).setMessage("操作失败，参数错误");
        if (!operator.isAdmin())
            return new Result(Result.FAIL).setErrorCode(1010190205).setMessage("操作失败，权限错误");
        AdminOpsRecord res = selectOne(id);
        if (res == null)
            return new Result(Result.FAIL).setErrorCode(1010190106).setMessage("数据库操作失败");
        Result<User> creatorResult = userService.readUserWithAllFalse(res.getCreatorId(), operator);
        if (creatorResult.getState() == Result.SUCCESS)
            res.setCreator(creatorResult.getObject());
        return new Result(Result.SUCCESS).setObject(res);
    }

    /**
     * 根据条件读取多条管理员操作记录
     *
     * @param param
     * @param operator
     * @return
     */
    @SneakyThrows
    public Result<List<AdminOpsRecord>> readRecordList(AdminOpsRecord param, Operator operator) {
        if (!param.isValidReadIndexParam())
            return new Result(Result.FAIL).setErrorCode(1010190207).setMessage("操作失败，参数错误");
        if (!operator.isAdmin())
            return new Result(Result.FAIL).setErrorCode(1010190208).setMessage("操作失败，权限错误");
        if (param.getQueryParam().getStartId().equals(0))
            param.getQueryParam().setStartId(Integer.MAX_VALUE);
        List<AdminOpsRecord> resList = selectList(param);
        if (resList == null)
            return new Result(Result.FAIL).setErrorCode(1010190109).setMessage("数据库操作失败");
        userService.multiSetUser(resList, AdminOpsRecord.class.getMethod("getCreatorId"), AdminOpsRecord.class.getMethod("setCreator", User.class));
        return new Result(Result.SUCCESS).setObject(resList);
    }
}
