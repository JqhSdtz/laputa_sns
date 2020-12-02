const user_id = getParamOfUrl('user_id');
const categoryTp = template(document.getElementById('categoryTp').innerHTML);

lpt.permissionServ.getForUser({
    data: {
        user_id: user_id
    },
    success: function (result) {
        initNav(result.operator);
        $('#category-list').append(categoryTp({list: result.object}));
    },
    fail: function (result) {
        alert(result.message);
    }
});