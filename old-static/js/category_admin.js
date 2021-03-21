const category_id = getParamOfUrl('category_id');
const operatorLevel = parseInt(getParamOfUrl('operator_level'));//模板中使用的变量
const userTp = template(document.getElementById('userTp').innerHTML);

lpt.permissionServ.getForCategory({
    data: {
        category_id:  category_id
    },
    success: function (result) {
        initNav(result.operator);
        $('#user-list').append(userTp({list: result.object}));
    },
    fail: function (result) {
        alert(result.message);
    }
});

function delAdmin(userId) {
    const opComment = prompt("请输入撤销理由(5-256字)", "");
    lpt.permissionServ.delAdmin({
        data: {
            user_id: userId,
            category_id: category_id,
            op_comment: opComment
        },
        success: function (result) {
            alert("操作成功，操作号" + result.object);
        },
        fail: function (result) {
            alert(result.message);
        }
    });
}

function updateAdmin(userId) {
    const targetLevel = prompt("请输入更改等级(1-4)", "");
    const opComment = prompt("请输入更改理由(5-256字)", "");
    lpt.permissionServ.updateAdmin({
        data: {
            user_id: userId,
            category_id: category_id,
            level: targetLevel,
            op_comment: opComment
        },
        success: function (result) {
            alert("操作成功，操作号" + result.object);
        },
        fail: function (result) {
            alert(result.message);
        }
    });
}