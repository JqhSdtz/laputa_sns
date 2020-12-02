const postTp = template(document.getElementById('postTp').innerHTML);

lpt.newsServ.query({
    success: function (result) {
        processPostList(result.object);
        initNav(result.operator);
        $('#unread_news_cnt').hide();
    },
    fail: function (result) {
        alert(result.message);
    }
});

function processPostList(list) {
    for (let i = 0; i < list.length; ++i) {
        const id = typeof list[i].content === 'undefined' ? 'undefined' : list[i].content.id;
        console.log('post ' + id);
    }
    console.log('-------------------');
    $('#post').append(postTp({
        contentDropTp: contentDropTp,
        commentOfPostTp: commentOfPostTp,
        forwardOfPostTp: forwardOfPostTp,
        list: list
    }));
}

$(document).scroll(function () {
    const scrollTop = $(document).scrollTop();
    const viewHeight = $(window).height();
    const contentHeight = $(document).height();
    if (contentHeight - scrollTop - viewHeight < 50 && !lpt.newsServ.querior.isSendingAjax)
        getNews();
});

function getNews() {
    if (!lpt.newsServ.querior.hasReachedBottom) {
        lpt.newsServ.query({
            success: function (result) {
                processPostList(result.object);
            },
            fail: function (result) {
                alert(result.message);
            }
        });
    }
}