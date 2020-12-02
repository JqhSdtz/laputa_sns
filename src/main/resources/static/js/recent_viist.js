const categoryTp = template(document.getElementById('categoryTp').innerHTML);

lpt.userServ.getRecentVisit({
    success: function (result) {
        initNav(result.operator);
        $('#category-list').append(categoryTp({list: result.object}));
    },
    fail: function (result) {
        alert(result.message);
    }
});

function pin(categoryId) {
    lpt.userServ.pinRecentVisit({
        data: {
            categoryId: categoryId
        },
        success: function () {
            alert('固定成功');
        },
        fail: function (result) {
            alert(result.message);
        }
    });
}

function unpin(categoryId) {
    lpt.userServ.unpinRecentVisit({
        data: {
            categoryId: categoryId
        },
        success: function () {
            alert('取消固定成功');
        },
        fail: function (result) {
            alert(result.message);
        }
    });
}