const userId = parseInt(getParamOfUrl('user_id'));
const userTp = template(document.getElementById('userTp').innerHTML);

lpt.followServ.queryFollower({
    data: {
        target_id: userId
    },
    success: function (result) {
        initNav(result.operator);
        processUserList(result.object);
    },
    fail: function (result) {
        alert(result.message);
    }
});

function getNewFollower() {
    if (!lpt.followServ.querior.hasReachedBottom) {
        lpt.followServ.queryFollower({
            data: {
                target_id: userId
            },
            success: function (result) {
                processUserList(result.object);
            },
            fail: function (result) {
                alert(result.message);
            }
        });
    }
}

function processUserList(list) {
    for (let i = 0; i < list.length; ++i)
        console.log('user ' + list[i].follower_id);
    console.log('-------------------');
    $('#user-list').append(userTp({list: list}));
}

$(document).scroll(function () {
    const scrollTop = $(document).scrollTop();
    const viewHeight = $(window).height();
    const contentHeight = $(document).height();
    if (contentHeight - scrollTop - viewHeight < 50 && !lpt.followServ.querior.isSendingAjax)
        getNewFollower();
});
