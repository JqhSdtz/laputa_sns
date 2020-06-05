const categoryTp = template(document.getElementById('categoryTp').innerHTML);

$.ajax({
    type: 'GET',
    url: baseUrl + '/user/recent_visit_categories',
    dataType: 'json',
    success: function (result) {
        if (result.state === 1) {
            setOperator(result.operator);
            initNav(result.operator);
            $('#category-list').append(categoryTp({list: result.object}));
        } else
            alert(result.message);
    }
})

function pin(categoryId) {
    $.ajax({
        type: 'POST',
        url: baseUrl + '/user/pin_category/' + categoryId,
        dataType: 'json',
        success: function (result) {
            if (result.state === 1)
                alert('固定成功');
            else
                alert(result.message);
        }
    });
}

function unpin(categoryId) {
    $.ajax({
        type: 'POST',
        url: baseUrl + '/user/unpin_category/' + categoryId,
        dataType: 'json',
        success: function (result) {
            if (result.state === 1)
                alert('取消固定成功');
            else
                alert(result.message);
        }
    });
}