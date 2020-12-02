const parentId = getParamOfUrl("parent_id");

$('#submitButton').click(function () {
    const title = $('#title').val();
    const content = $('#content').val();
    if (content.length === 0) {
        alert('请输入名称');
        return;
    } else if (content.length > 256) {
        alert('简介过长');
        return;
    } else if (title.length > 10) {
        alert('名称不能超过10个字');
        return;
    }
    const opComment = prompt("请输入创建理由(5-256个字)", "");
    lpt.categoryServ.create({
        data: {
            name: title,
            intro: content,
            parent_id: parentId,
            type: parseInt($('#type-select').val()),
            op_comment: opComment
        },
        success: function () {
            alert('创建成功');
            $('#content').val('');
            $('#title').val('');
        },
        fail: function (result) {
            alert(result.message);
        }
    });
});