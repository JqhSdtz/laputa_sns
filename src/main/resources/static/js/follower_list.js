const userId = parseInt(getParamOfUrl('user_id'));
const userTp = template(document.getElementById('userTp').innerHTML);
let hasReachedBottom = false;
let curQueryToken;
let totalLength = 0;
let isSendingAjax = false;

$.ajax({
    type: 'POST',
    url: baseUrl + '/follow/follower',
    contentType: 'application/json',
    dataType: 'json',
    data: JSON.stringify({
        target_id: userId,
        query_param: {
            query_num: 10
        }
    }),
    success: function (result) {
        if (result.state === 1) {
            setOperator(result.operator);
            initNav(result.operator);
            curQueryToken = result.attached_token;
            processUserList(result.object);
        } else
            alert(result.message);
    }
});

function getNewFollower() {
    if (!hasReachedBottom) {
        if (isSendingAjax)
            return;
        isSendingAjax = true;
        $.ajax({
            type: 'POST',
            url: baseUrl + '/follow/follower',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                target_id: userId,
                query_param: {
                    query_token: curQueryToken,
                    query_num: 10
                }
            }),
            success: function (result) {
                if (result.state === 0) {
                    alert(result.message);
                    return;
                }
                curQueryToken = result.attached_token;
                if (result.object.length === 0)
                    hasReachedBottom = true;
                processUserList(result.object);
            },
            complete: function () {
                isSendingAjax = false;
            }
        });
    }
}

function processUserList(list) {
    for (let i = 0; i < list.length; ++i)
        console.log('user ' + list[i].follower_id);
    totalLength += list.length;
    console.log('-------------------');
    $('#user-list').append(userTp({list: list}));
}

$(document).scroll(function () {
    const scrollTop = $(document).scrollTop();
    const viewHeight = $(window).height();
    const contentHeight = $(document).height();
    if (contentHeight - scrollTop - viewHeight < 50 && !isSendingAjax)
        getNewFollower();
});
