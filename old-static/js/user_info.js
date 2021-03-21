const postTp = template(document.getElementById('postTp').innerHTML);
const user_id = parseInt(getParamOfUrl('user_id'));

lpt.userServ.get({
    data: {
        user_id: user_id
    },
    success: function (result) {
        $('title').html(result.object.nick_name);
        showUserDetail(result.object);
        initNav(result.operator);
    },
    fail: function (result) {
        alert(result.message);
    }
});

lpt.postServ.queryForCreator({
    data: {
        creator_id: user_id
    },
    success: function (result) {
        processPostList(result.object);
    },
    fail: function (result) {
        alert(result.message);
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
    if (contentHeight - scrollTop - viewHeight < 50 && !lpt.postServ.querior.isSendingAjax)
        getNews();
});

function processPostList(list) {
    for (let i = 0; i < list.length; ++i)
        console.log('post ' + list[i].id);
    console.log('-------------------');
    $('#post').append(postTp({
        contentDropTp: contentDropTp,
        commentOfPostTp: commentOfPostTp,
        forwardOfPostTp: forwardOfPostTp,
        list: list
    }));
}

function getNews() {
    if (!lpt.postServ.querior.hasReachedBottom) {
        lpt.postServ.queryForCreator({
            data: {
                creator_id: user_id
            },
            success: function (result) {
                processPostList(result.object);
            },
            fail: function (result) {
                alert(result.message);
            }
        });
    }
}

function followOrCancel(elem) {
    const isCancel = $(elem).attr('data-followed') === '1';
    const doAction = isCancel ? lpt.followServ.unFollow : lpt.followServ.follow;
    doAction({
        data: {
            type: 0,
            target_id: user_id
        },
        success: function () {
            alert(isCancel ? '取关成功' : '关注成功');
            $(elem).attr('data-followed', isCancel ? '0' : '1');
            $(elem).text(isCancel ? '关注' : '取消关注');
            const oriCnt = parseInt($('#follower-cnt').text());
            $('#follower-cnt').text(oriCnt + (isCancel ? -1 : 1));
        },
        fail: function (result) {
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
    const doAction = isCancel ? lpt.userServ.cancelTopPost : lpt.userServ.setTopPost;
    doAction({
        data: {
            top_post_id: postId
        },
        success: function () {
            alert('设置成功');
            $(elem).text(isCancel ? '置顶' : '取消置顶');
        },
        fail: function (result) {
            alert(result.message);
        }
    });
}