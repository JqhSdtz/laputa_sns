package com.laputa.laputa_sns.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.laputa.laputa_sns.common.AbstractBaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author JQH
 * @since 下午 3:50 20/04/24
 */

@Getter
@Setter
@Accessors(chain = true)
public class AdminOpsRecord extends AbstractBaseEntity {

    public static final int TYPE_DELETE_POST = 0;
    public static final int TYPE_DELETE_CML1 = 1;
    public static final int TYPE_DELETE_CML2 = 2;
    public static final int TYPE_CREATE_CATEGORY = 3;
    public static final int TYPE_DELETE_CATEGORY = 4;
    public static final int TYPE_UPDATE_CATEGORY_PARENT = 5;
    public static final int TYPE_UPDATE_CATEGORY_INFO = 6;
    public static final int TYPE_SET_CATEGORY_TOP_POST = 7;
    public static final int TYPE_CANCEL_CATEGORY_TOP_POST = 8;
    public static final int TYPE_SET_CATEGORY_DEF_SUB = 9;
    public static final int TYPE_CANCEL_CATEGORY_DEF_SUB = 10;
    public static final int TYPE_UPDATE_CATEGORY_DISP_SEQ = 11;
    public static final int TYPE_UPDATE_CATEGORY_CACHE_NUM = 12;
    public static final int TYPE_CREATE_PERMISSION = 13;
    public static final int TYPE_UPDATE_PERMISSION = 14;
    public static final int TYPE_DELETE_PERMISSION = 15;

    private String entityType = "ADMIN_REC";

    private static final String[] typeStr = {"delete_post", "delete_cml1", "delete_cml2", "create_category", "delete_category", "update_category_parent",
            "update_category_info", "set_category_top_post", "cancel_category_top_post", "set_category_def_sub", "cancel_category_def_sub", "update_category_disp_seq",
            "update_category_cache_num", "create_permission", "update_permission", "delete_permission"};

    private User creator;

    @JsonProperty("target_id")
    private Integer targetId;

    @JsonProperty("target")
    private Object target;

    private String comment;

    private String desc;

    @JsonProperty("type_str")
    public String getTypeStr() {
        return typeStr[type];
    }

    @JsonIgnore
    public Integer getCreatorId() {
        return creator == null ? null : creator.getId();
    }

    @JsonIgnore
    public boolean isValidInsertParam() {
        return targetId != null || opComment != null;
    }

    @JsonIgnore
    public boolean isValidReadIndexParam() {
//        if (targetId == null && (creator == null || creator.getId() == null) && type == null)
//            return false;
        return queryParam != null && queryParam.getStartId() != null && queryParam.getQueryNum() != null && queryParam.getQueryNum() <= 10;
    }
}
