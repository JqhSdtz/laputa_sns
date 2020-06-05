const postTp = template(document.getElementById('postTp').innerHTML);
let curSortType = 'popular';
const urlIdParam = getParamOfUrl('category_id')
const categoryId = urlIdParam === '' ? 0 : parseInt(urlIdParam);
let curQueryToken;
let hasReachedBottom = false;
let totalLength = 0;

$.ajax({
    type: 'POST',
    url: baseUrl + '/post/popular',
    contentType: 'application/json',
    dataType: 'json',
    data: JSON.stringify({
        category_id: categoryId,
        query_param: {
            query_num: 10
        }
    }),
    success: function (result) {
        setOperator(result.operator);
        if (result.state === 0) {
            alert(result.message);
            return;
        }
        curQueryToken = result.attached_token;
        processPostList(result.object);
        initNav(result.operator);
    }
});

$(document).scroll(function () {
    const scrollTop = $(document).scrollTop();
    const viewHeight = $(window).height();
    const contentHeight = $(document).height();
    if (contentHeight - scrollTop - viewHeight < 50 && !isSendingAjax)
        getNewPosts();
});

function getNewPosts() {
    if (!hasReachedBottom) {
        if (isSendingAjax)
            return;
        isSendingAjax = true;
        $.ajax({
            type: 'POST',
            url: baseUrl + '/post/' + curSortType,
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                category_id: categoryId,
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

$('#anotherSortType').click(function () {
    if (curSortType === 'popular') {
        curSortType = 'latest';
        $('#changeSortTypeBtn').text('按时间');
        $('#anotherSortType').text('按热度');
    } else {
        curSortType = 'popular';
        $('#changeSortTypeBtn').text('按热度');
        $('#anotherSortType').text('按时间');
    }
    $('#post').html('');
    totalLength = 0;
    curQueryToken = null;
    hasReachedBottom = false;
    commentClear();
    forwardClear();
    getNewPosts();
});

function processPostList(list) {
    for (let i = 0; i < list.length; ++i)
        console.log('post ' + list[i].id);
    totalLength += list.length;
    console.log('-------------------');
    $('#post').append(postTp({contentDropTp: contentDropTp, commentOfPostTp: commentOfPostTp, forwardOfPostTp: forwardOfPostTp, list: list}));
}

function setPostTop(elem, isCancel) {
    const postId = parseInt($(elem).attr('data-id'));
    const comment = prompt("输入置顶原因(5-256个字)", "");
    $.ajax({
        type: 'PATCH',
        url: baseUrl + '/category/top_post/' + (isCancel ? 'cancel' : 'create'),
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({id: categoryId, top_post_id: postId, op_comment: comment}),
        success: function (result) {
            if (result.state === 1) {
                alert('设置成功');
                $(elem).text(isCancel ? '置顶' : '取消置顶');
            } else
                alert(result.message);
        }
    });
}
