const userId = getParamOfUrl('user_id');
const userTp = template(document.getElementById('userTp').innerHTML);

$.ajax({
    type: 'GET',
    url: baseUrl + '/follow/following/' + userId,
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
