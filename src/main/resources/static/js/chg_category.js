const category_id = getParamOfUrl('category_id');

$('#submit-button').click(function () {
    lpt.categoryServ.setParent({
        data: {
            id: category_id,
            parent_id: selectedCategoryId
        },
        success: function () {
            alert('更改成功');
        },
        fail: function (result) {
            alert(result.message);
        }
    });
});