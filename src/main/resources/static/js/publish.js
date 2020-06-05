let isSendingAjax = false;

$('#submitButton').click(function () {
    if (isSendingAjax)
        return;
    const title = $('#title').val();
    const content = $('#content').val();
    if (content.length < 10) {
        alert('帖子长度不能小于10');
        return;
    } else if (content.length > 100000) {
        alert('帖子过长，请分开发送');
        return;
    } else if (title.length > 20) {
        alert('标题长度不能大于20');
        return;
    } else if (selectedCategoryId === null) {
        alert('请选择目录');
        return;
    }
    isSendingAjax = true;
    $.ajax({
        type: 'POST',
        url: baseUrl + '/post/public',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            category_id: selectedCategoryId,
            title: title,
            content: content,
            raw_img: rawImg
        }),
        success: function (data) {
            if (data.state == 1) {
                alert('发帖成功');
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

let imgUrl = baseUrl + '/oss/pst';
let uploadLmt = 9;