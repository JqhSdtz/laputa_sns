let imgUrl = baseUrl + '/oss/ava';
let _operator = getOperator();
initNav(_operator);

$('#submit-btn').click(function () {
    lpt.userServ.setInfo({
        data: {
            raw_avatar: rawImg
        },
        success: function () {
            alert('修改成功');
        },
        fail: function (result) {
            alert(result.message);
        }
    });
});