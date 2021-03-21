const noticeTp = template(document.getElementById('noticeTp').innerHTML);
const noticeMap = new Map();

lpt.noticeServ.query({
    success: function (result) {
        initNav(result.operator);
        processNoticeList(result.object);
    },
    fail: function (result) {
        alert(result.message);
    }
});

function getNewNotice() {
    if (!lpt.noticeServ.querior.hasReachedBottom) {
        lpt.noticeServ.query({
            success: function (result) {
                if (result.object.length === 0) {
                    $('#more-btn').text('没有更多');
                } else {
                    processNoticeList(result.object);
                }
            },
            fail: function (result) {
                alert(result.message);
            }
        });
    }
}

function processNoticeList(list) {
    for (let i = 0; i < list.length; ++i) {
        noticeMap.set(getNoticeKey(list[i]), list[i]);
        console.log('notice ' + list[i].content_id);
    }
    console.log('-------------------');
    $('#notice-list').append(noticeTp({list: list}));
}

$('#more-btn').click(function () {
    getNewNotice();
});

function getDescription(notice) {
    const typeStr = notice.type_str;
    const title = (typeof notice.content === 'undefined' || notice.content == null) ? null : notice.content.title;
    let desc = '';
    if (typeof title !== 'undefined' && title !== null)
        desc = title;
    else if (notice.content != null)
        desc = notice.content.content;
    switch (typeStr) {
        case 'like_post':
            return '帖子"' + desc + '"收到' + notice.unread_cnt + '条赞';
        case 'like_cml1':
        case 'like_cml2':
            return '评论"' + desc + '"收到' + notice.unread_cnt + '条赞';
        case 'cml1_of_post':
            return '帖子"' + desc + '"收到' + notice.unread_cnt + '条评论';
        case 'cml2_of_cml1':
            return '评论"' + desc + '"收到' + notice.unread_cnt + '条回复';
        case 'fw_post':
            return '帖子"' + desc + '"收到' + notice.unread_cnt + '条转发';
        case 'follower':
            return '新增' + notice.unread_cnt + '位粉丝';
        case 'reply_of_cml2':
            return '评论"' + desc + '"收到' + notice.unread_cnt + '条回复';
    }
}

function showNoticeDetail(key) {
    const notice = noticeMap.get(key);
    lpt.noticeServ.markAsRead({
        data: {
            type: notice.type,
            content_id: notice.content_id
        },
        fail: function (result) {
            alert(result.message);
        }
    });
    switch (notice.type_str) {
        case 'like_post':
            window.location.href = 'like_list.html?type=post&id=' + notice.content.id;
            break;
        case 'like_cml1':
            window.location.href = 'like_list.html?type=cml1&id=' + notice.content.id;
            break;
        case 'like_cml2':
            window.location.href = 'like_list.html?type=cml2&id=' + notice.content.id;
            break;
        case 'follower':
            window.location.href = 'follower_list.html?user_id=' + lpt.operatorServ.getCurrent().user_id;
    }
}

function getNoticeKey(notice) {
    return notice.type + ':' + notice.content_id;
}