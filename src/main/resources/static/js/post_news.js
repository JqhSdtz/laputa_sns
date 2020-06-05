const postTp = template(document.getElementById('postTp').innerHTML);
let curQueryToken;
let hasReachedBottom = false;
let totalLength = 0;

$.ajax({
    type: 'POST',
    url: baseUrl + '/news',
    contentType: 'application/json',
    dataType: 'json',
    data: JSON.stringify({
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
        initNav(result.operator);
        $('#unread_news_cnt').hide();
    }
});

function processPostList(list) {
    for (let i = 0; i < list.length; ++i) {
        const id = typeof list[i].content === 'undefined' ?  'undefined' : list[i].content.id;
        console.log('post ' + id);
    }
    totalLength += list.length;
    console.log('-------------------');
    $('#post').append(postTp({contentDropTp: contentDropTp, commentOfPostTp: commentOfPostTp, forwardOfPostTp: forwardOfPostTp, list: list}));
}

$(document).scroll(function () {
    const scrollTop = $(document).scrollTop();
    const viewHeight = $(window).height();
    const contentHeight = $(document).height();
    if (contentHeight - scrollTop - viewHeight < 50 && !isSendingAjax)
        getNews();
});

function getNews() {
    if (!hasReachedBottom) {
        if (isSendingAjax)
            return;
        isSendingAjax = true;
        $.ajax({
            type: 'POST',
            url: baseUrl + '/news',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
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