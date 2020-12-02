const postTp = template(document.getElementById('postTp').innerHTML);
let curSortType = 'popular';
const urlIdParam = getParamOfUrl('category_id');
const categoryId = urlIdParam === '' ? 0 : parseInt(urlIdParam);

getNewPosts(true);

$(document).scroll(function () {
    const scrollTop = $(document).scrollTop();
    const viewHeight = $(window).height();
    const contentHeight = $(document).height();
    if (contentHeight - scrollTop - viewHeight < 50 && !isSendingAjax)
        getNewPosts();
});

function getNewPosts(isInitNav) {
    if (!lpt.postServ.querior.hasReachedBottom) {
        lpt.postServ.queryForCategory({
            data: {
                queryType: curSortType,
                category_id: categoryId
            },
            success: function (result) {
                processPostList(result.object);
                if (isInitNav)
                    initNav(result.operator);
            },
            fail: function (result) {
                alert(result.message);
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
    lpt.postServ.querior.reset();
    commentClear();
    forwardClear();
    getNewPosts();
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

function setPostTop(elem, isCancel) {
    const postId = parseInt($(elem).attr('data-id'));
    const comment = prompt("输入置顶原因(5-256个字)", "");
    lpt.categoryServ.setTopPost({
        data: {
            isCancel: isCancel,
            id: categoryId,
            top_post_id: postId,
            op_comment: comment
        },
        success: function() {
            alert('设置成功');
            $(elem).text(isCancel ? '置顶' : '取消置顶');
        },
        fail: function(result) {
            alert(result.message);
        }
    });
}
