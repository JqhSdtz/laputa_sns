let isSendingAjax = false;
const parentId = getParamOfUrl("parent_id");

$('#submitButton').click(function () {
    if (isSendingAjax)
        return;
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
    isSendingAjax = true;
    $.ajax({
        type: 'POST',
        url: baseUrl + '/category',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            name: title,
            intro: content,
            parent_id: parentId,
            type: parseInt($('#type-select').val()),
            op_comment: opComment
        }),
        success: function (data) {
            if (data.state == 1) {
                alert('创建成功');
                $('#content').val('');
                $('#title').val('');
            } else {
                alert(data.message);
            }
        },
        complete: function () {
            isSendingAjax = false;
        }
    });
});