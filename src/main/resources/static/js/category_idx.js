const categoryTp = template(document.getElementById('categoryTp').innerHTML);
const categoryTypeStr = ["普通目录", "私有目录", "默认子目录"];//模板中使用的变量
const urlIdParam = getParamOfUrl('root_id');
const rootId = urlIdParam === '' ? 0 : parseInt(urlIdParam);
let rights;

$.ajax({
    type: 'GET',
    url: baseUrl + '/category/' + rootId,
    dataType: 'json',
    success: function (result) {
        if (result.state === 1) {
            $('#root-card').html(categoryTp({category: result.object.root}));
            if (typeof result.object.rights !== 'undefined')
                setRights(result.object.rights);
            $('title').html(result.object.root.name);
            const subList = result.object.sub_list;
            for (let i = 0; i < subList.length; ++i)
                $('#categories').append(categoryTp({category: subList[i]}));
            initNav(result.operator);
        } else
            alert(result.message);
    }
});

function setRights(_rights) {
    if (_rights === null)
        return;
    rights = _rights;
    const rightsCard = $('#op-card');
    if (rights.create)
        rightsCard.append('<button onclick="createSub()" class="op-btn btn btn-primary">创建子目录</button>');
    if (rights.delete)
        rightsCard.append('<button onclick="deleteCategory()" class="op-btn btn btn-primary">删除目录</button>');
    if (rights.update_disp_seq)
        rightsCard.append('<button onclick="updateCategoryDispSeq()" class="op-btn btn btn-primary">设置目录展示顺序</button>');
    if (rights.update_cache_num)
        rightsCard.append('<button onclick="updateCategoryCacheNum()" class="op-btn btn btn-primary">设置索引缓存长度</button>');
    if (rights.update_parent)
        rightsCard.append('<button onclick="changeParent()" class="op-btn btn btn-primary">更改父目录</button>');
    if (rights.def_sub)
        rightsCard.append('<button onclick="setDefaultSubCategory()" class="op-btn btn btn-primary">设置默认子目录</button>');
    if (rights.this_level && rights.this_level > 1)
        rightsCard.append('<button onclick="showAdmins()" class="op-btn btn btn-primary">查看管理员列表</button>');
    if (rights.parent_level && rights.parent_level > 1)
        rightsCard.append('<button onclick="setAdmin()" class="op-btn btn btn-primary">设置管理员</button>');
}

function createSub() {
    window.open('new_category.html?parent_id=' + rootId, "_blank");
}

function showAdmins() {
    const level = rights.parent_level ? rights.parent_level : 0;
    window.open("category_admin.html?category_id=" + rootId + "&operator_level=" + level)
}

function deleteCategory() {
    const opComment = prompt("请输入删除理由", "");
    $.ajax({
        type: 'DELETE',
        url: baseUrl + '/category/',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
           id: rootId,
           op_comment: opComment
        }),
        success: function (result) {
            if (result.state === 1) {
                alert('删除成功，操作号' + result.object);
            } else
                alert(result.message);
        }
    });
}

function changeParent() {
    window.location.href = 'chg_category.html?category_id=' + rootId;
}

function updateCategoryCacheNum() {
    const cacheNum = prompt("请输入缓存长度(100-10000)", "");
    const opComment = prompt("请输入操作理由", "");
    $.ajax({
        type: 'PATCH',
        url: baseUrl + '/category/cache_num',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            id: rootId,
            cache_num: cacheNum,
            op_comment: opComment
        }),
        success: function (result) {
            if (result.state === 1) {
                alert('操作成功，操作号' + result.object);
            } else
                alert(result.message);
        }
    });
}

function updateCategoryDispSeq() {
    const dispSeq = prompt("请输入展示顺序", "");
    const opComment = prompt("请输入操作理由", "");
    $.ajax({
        type: 'PATCH',
        url: baseUrl + '/category/disp_seq',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            id: rootId,
            disp_seq: dispSeq,
            op_comment: opComment
        }),
        success: function (result) {
            if (result.state === 1) {
                alert('操作成功，操作号' + result.object);
            } else
                alert(result.message);
        }
    });
}

function setDefaultSubCategory() {
    const defSubId = prompt("请输入子目录ID", "");
    const opComment = prompt("请输入操作理由", "");
    $.ajax({
        type: 'PATCH',
        url: baseUrl + '/category/def_sub',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            id: rootId,
            def_sub_id: defSubId,
            op_comment: opComment
        }),
        success: function (result) {
            if (result.state === 1) {
                alert('操作成功，操作号' + result.object);
            } else
                alert(result.message);
        }
    });
}

function setAdmin() {
    const targetId = prompt("请输入目标用户的ID", "");
    const targetLevel = prompt("请输入设置等级(不能高于设置者父目录权限等级)", "");
    const opComment = prompt("请输入操作理由(5-256个字)", "");
    $.ajax({
        type: 'POST',
        url: baseUrl + '/permission',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            user_id: targetId,
            category_id: rootId,
            level: targetLevel,
            op_comment: opComment
        }),
        success: function (result) {
            if (result.state === 1)
                alert("操作成功，操作号" + result.object)
            else
                alert(result.message)
        }
    });
}