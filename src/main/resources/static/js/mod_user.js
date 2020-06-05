let imgUrl = baseUrl + '/oss/ava';
let _operator = getOperator();
initNav(_operator);

$('#submit-btn').click(function () {
    $.ajax({
       type: 'PATCH',
       url: baseUrl + '/user/info',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            id: _operator.user_id,
            raw_avatar: rawImg
        }),
        success: function (result) {
            if (result.state === 1) {
                alert('修改成功');
                setOperator(result.operator);
            } else
                alert(result.message);
        }
    });
});