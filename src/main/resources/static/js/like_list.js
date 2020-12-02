const id = getParamOfUrl('id');
const type = getParamOfUrl('type');
const userTp = template(document.getElementById('userTp').innerHTML);

lpt.likeServ.query({
    data: {
        type: type,
        target_id: id
    },
    success: function (result) {
        initNav(result.operator);
        processUserList(result.object);
    },
    fail: function (result) {
        alert(result.message);
    }
});

function getNewLikeRecord() {
    if (!lpt.likeServ.querior.hasReachedBottom) {
        lpt.likeServ.query({
            data: {
                type: type,
                target_id: id
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
        console.log('user ' + list[i].creator_id);
    console.log('-------------------');
    $('#user-list').append(userTp({list: list}));
}

$(document).scroll(function () {
    const scrollTop = $(document).scrollTop();
    const viewHeight = $(window).height();
    const contentHeight = $(document).height();
    if (contentHeight - scrollTop - viewHeight < 50 && !lpt.likeServ.querior.isSendingAjax)
        getNewLikeRecord();
});
