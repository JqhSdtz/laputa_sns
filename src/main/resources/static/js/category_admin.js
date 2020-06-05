const category_id = getParamOfUrl('category_id');
const operatorLevel = parseInt(getParamOfUrl('operator_level'));//模板中使用的变量
const userTp = template(document.getElementById('userTp').innerHTML);

$.ajax({
    type: 'GET',
    url: baseUrl + '/permission/category/' + category_id,
    dataType: 'json',
    success: function (result) {
        if (result.state === 1) {
            setOperator(result.operator);
            initNav(result.operator);
            $('#user-list').append(userTp({list: result.object}));
        } else
            alert(result.message);
    }
});

function delAdmin(userId) {
    const opComment = prompt("请输入撤销理由(5-256字)", "");
    $.ajax({
        type: 'DELETE',
        url: baseUrl + '/permission',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            user_id: userId,
            category_id: category_id,
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

function updateAdmin(userId) {
    const targetLevel = prompt("请输入更改等级(1-4)", "");
    const opComment = prompt("请输入更改理由(5-256字)", "");
    $.ajax({
        type: 'PATCH',
        url: baseUrl + '/permission',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            user_id: userId,
            category_id: category_id,
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