const userId = getParamOfUrl('user_id');
const userTp = template(document.getElementById('userTp').innerHTML);

lpt.followServ.getFollowing({
    data: {
        userId: userId
    },
    success: function (result) {
        initNav(result.operator);
        $('#user-list').append(userTp({list: result.object}));
    },
    fail: function (result) {
        alert(result.message);
    }
});
