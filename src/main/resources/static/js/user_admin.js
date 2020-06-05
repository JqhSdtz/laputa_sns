const user_id = getParamOfUrl('user_id');
const categoryTp = template(document.getElementById('categoryTp').innerHTML);

$.ajax({
    type: 'GET',
    url: baseUrl + '/permission/user/' + user_id,
    dataType: 'json',
    success: function (result) {
        if (result.state === 1) {
            setOperator(result.operator);
            initNav(result.operator);
            $('#category-list').append(categoryTp({list: result.object}));
        } else
            alert(result.message);
    }
});