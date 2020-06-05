const postTp = template(document.getElementById('postTp').innerHTML);
let curQueryToken;
let hasReachedBottom = false;
let totalLength = 0;
const user_id = parseInt(getParamOfUrl('user_id'));

$.ajax({
    type: 'GET',
    url: baseUrl + '/user/' + user_id,
    dataType: 'json',
    success: function (result) {
        setOperator(result.operator)
        if (result.state === 0) {
            alert(result.message);
            return;
        }
        $('title').html(result.object.nick_name);
        showUserDetail(result.object);
        initNav(result.operator);
    }
});

$.ajax({
    type: 'POST',
    url: baseUrl + '/post/creator',
    contentType: 'application/json',
    dataType: 'json',
    data: JSON.stringify({
        creator_id: user_id,
        query_param: {
            query_num: 10
        }
    }),
    success: function (result) {
        setOperator(result.operator)
        if (result.state === 0) {
            alert(result.message);
            return;
        }
        curQueryToken = result.attached_token;
        processPostList(result.object);
    }
});

function showUserDetail(user) {
    $('#user-name').text(user.nick_name);
    $('#user-id').text(user.id);
    $('#following-cnt').text(user.following_cnt);
    $('#follower-cnt').text(user.followers_cnt);
    $('#post-cnt').text(user.post_cnt);
    $('#user-intro').text(user.intro);
    if (user.followed_by_viewer)
        $('#follow-btn').attr('data-followed', '1').text("取消关注");
    else
        $('#follow-btn').attr('data-followed', '0').text("关注");
    if (isAdmin())
        $('#show-admin-perm').show().click(function () {
            window.open("user_admin.html?user_id=" + user.id, "_blank")
        });
}

$(document).scroll(function () {
    const scrollTop = $(document).scrollTop();
    const viewHeight = $(window).height();
    const contentHeight = $(document).height();
    if (contentHeight - scrollTop - viewHeight < 50 && !isSendingAjax)
        getNews();
});

function processPostList(list) {
    for (let i = 0; i < list.length; ++i)
        console.log('post ' + list[i].id);
    totalLength += list.length;
    console.log('-------------------');
    $('#post').append(postTp({contentDropTp: contentDropTp, commentOfPostTp: commentOfPostTp, forwardOfPostTp: forwardOfPostTp, list: list}));
}

function getNews() {
    if (!hasReachedBottom) {
        if (isSendingAjax)
            return;
        isSendingAjax = true;
        $.ajax({
            type: 'POST',
            url: baseUrl + '/post/creator',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                creator_id: user_id,
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
                processPostList(result.object);
            },
            complete: function () {
                isSendingAjax = false;
            }
        });
    }
}

function followOrCancel(elem) {
    const isCancel = $(elem).attr('data-followed') === '1';
    $.ajax({
       type: isCancel ? 'DELETE' : 'POST',
       url: baseUrl + '/follow',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
           type: 0,
           target_id: user_id
        }),
        success: function (result) {
            if (result.state === 1) {
                alert(isCancel ? '取关成功' : '关注成功');
                $(elem).attr('data-followed', isCancel ? '0' : '1');
                $(elem).text(isCancel ? '关注' : '取消关注');
                const oriCnt = parseInt($('#follower-cnt').text());
                $('#follower-cnt').text(oriCnt + (isCancel ? -1 : 1));
            } else
                alert(result.message);
        }
    });
}

function showFollowingList() {
    window.location.href = baseStaticUrl + '/following_list.html?user_id=' + user_id;
}

function showFollowerList() {
    window.location.href = baseStaticUrl + '/follower_list.html?user_id=' + user_id;
}

function setPostTop(elem, isCancel) {
    const postId = parseInt($(elem).attr('data-id'));
    $.ajax({
        type: 'PATCH',
        url: baseUrl + '/user/top_post/' + (isCancel ? 'cancel' : 'create'),
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({top_post_id: postId}),
        success: function (result) {
            if (result.state === 1) {
                alert('设置成功');
                $(elem).text(isCancel ? '置顶' : '取消置顶');
            } else
                alert(result.message);
        }
    });
}