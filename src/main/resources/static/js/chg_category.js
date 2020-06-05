const category_id = getParamOfUrl('category_id');

$('#submit-button').click(function () {
    $.ajax({
       type: 'PATCH',
       url: baseUrl + '/category/parent',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            id: category_id,
            parent_id: selectedCategoryId
        }),
        success: function (data) {
            if (data.state == 1) {
                alert('更改成功');
            } else {
                alert(data.message);
            }
        },
    });
});