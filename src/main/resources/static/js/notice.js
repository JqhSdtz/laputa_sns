const noticeTp = template(document.getElementById('noticeTp').innerHTML);
let hasReachedBottom = false;
let curFrom = 0;
let totalLength = 0;
let isSendingAjax = false;
const noticeMap = new Map();

$.ajax({
    type: 'POST',
    url: baseUrl + '/notice',
    contentType: 'application/json',
    dataType: 'json',
    data: JSON.stringify({
        from: curFrom,
        query_num: 10
    }),
    success: function (result) {
        if (result.state === 1) {
            setOperator(result.operator);
            initNav(result.operator);
            curFrom += result.object.length;
            processNoticeList(result.object);
        } else
            alert(result.message);
    }
});

function getNewNotice() {
    if (!hasReachedBottom) {
        if (isSendingAjax)
            return;
        isSendingAjax = true;
        $.ajax({
            type: 'POST',
            url: baseUrl + '/notice',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                from: curFrom,
                query_num: 10
            }),
            success: function (result) {
                if (result.state === 0) {
                    alert(result.message);
                    return;
                }
                curFrom += result.object.length;
                if (result.object.length === 0) {
                    hasReachedBottom = true;
                    $('#more-btn').text('没有更多');
                }
                processNoticeList(result.object);
            },
            complete: function () {
                isSendingAjax = false;
            }
        });
    }
}

function processNoticeList(list) {
    for (let i = 0; i < list.length; ++i) {
        noticeMap.set(getNoticeKey(list[i]), list[i]);
        console.log('notice ' + list[i].content_id);
    }
    totalLength += list.length;
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
    $.ajax({
        type: 'POST',
        url: baseUrl + '/notice/read',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            type: notice.type,
            content_id: notice.content_id
        }),
        success: function (result) {
            if (result.state === 0)
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
            window.location.href = 'follower_list.html?user_id=' + getOperator().user_id;
    }
}

function getNoticeKey(notice) {
    return notice.type + ':' + notice.content_id;
}