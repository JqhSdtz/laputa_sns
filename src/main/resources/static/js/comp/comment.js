$(document.body).append(getUrlSync(baseStaticUrl + '/comp/comment.html'));

const commentTp = template(document.getElementById('commentTp').innerHTML);
const cml2Tp = template(document.getElementById('cml2Tp').innerHTML);
const commentOfPostTp = template(document.getElementById('commentOfPostTp').innerHTML);

const commentQueryTokenMap = new Map();
const l1ListOfPostMap = new Map();
const commentHasReachedBottomFlagMap = new Map();
const commentRankType = new Map();

function commentClear() {
    commentQueryTokenMap.clear();
    commentHasReachedBottomFlagMap.clear();
    commentRankType.clear();
}

function processCommentList(id, list, type, clear) {
    for (let i = 0; i < list.length; ++i)
        console.log('cm' + type + ' ' + list[i].id);
    console.log('-------------------');
    const html = type === 'l1' ? commentTp({contentDropTp: contentDropTp, cml2Tp: cml2Tp, list: list}) : cml2Tp({contentDropTp: contentDropTp, list: list});
    if (clear)
        $('#comment_' + type + '_list_' + id).html('');
    $('#comment_' + type + '_list_' + id).append(html);
}

function getNewComments(parentId, elem, type, rankType, clear) {
    if (!commentHasReachedBottomFlagMap.get(type + parentId)) {
        if (isSendingAjax)
            return;
        isSendingAjax = true;
        $.ajax({
            type: 'POST',
            url: baseUrl + '/comment/' + type + '/' + rankType,
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                parent_id: parentId,
                query_param: {
                    query_token: commentQueryTokenMap.get(type + parentId),
                    query_num: 5
                }
            }),
            success: function (result) {
                if (result.state === 1) {
                    commentQueryTokenMap.set(type + parentId, result.attached_token);
                    if (type === 'l1') {
                        const cml1List = result.object;
                        const l1OfPostList = l1ListOfPostMap.has(parentId) ? l1ListOfPostMap.get(parentId) : new Array();
                        for (let i = 0; i < cml1List.length; ++i)
                            l1OfPostList.push(cml1List[i]);
                        l1ListOfPostMap.set(parentId, l1OfPostList);
                    }
                    if (result.object.length === 0) {
                        commentHasReachedBottomFlagMap.set(type + parentId, true);
                        $('#more_comment_' + type + '_btn_' + parentId).text('没有更多');
                    }
                    if (elem !== null)
                        $(elem).attr('data-gotten', '1');
                    commentRankType.set(parentId, rankType);
                    processCommentList(parentId, result.object, type, clear);
                } else
                    alert(result.message);
            },
            complete: function () {
                isSendingAjax = false;
            }
        });
    }
}

function showPostComment(elem, type, rankType) {
    const parentId = parseInt($(elem).attr('data-id'));
    const shown = $(elem).attr('data-shown');
    const gotten = $(elem).attr('data-gotten');
    const toggle_elem_id = type === 'l1' ? ('comment_l1_of_post_' + parentId) : ('comment_l2_of_l1_' + parentId);
    if ($('#comment_' + type + '_cnt_' + parentId).text() !== '0' && shown === '0' && gotten !== '1')
        getNewComments(parentId, elem, type, rankType, false);
    if (type === 'l1')
        toggleElem(toggle_elem_id, 'forward_of_post_' + parentId)
    else
        toggleElem(toggle_elem_id);
}

function readMoreComments(elem, type) {
    const parentId = parseInt($(elem).attr('data-id'));
    getNewComments(parentId, null, type, type === 'l1' ? commentRankType.get(parentId) : 'popular', false);
}

function sendComment(elem, type) {
    const parentId = parseInt($(elem).attr('data-id'));
    const inputElem = $('#comment_' + type + '_text_' + parentId);
    const text = inputElem.val();
    if (text.length === 0) {
        alert('请输入内容');
        return;
    } else if (text.length > 249) {
        alert('评论内容太长');
        return;
    }
    if (isSendingAjax)
        return;
    isSendingAjax = true;
    $.ajax({
        type: 'POST',
        url: baseUrl + '/comment/' + type,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({parent_id: parentId, content: text}),
        success: function (result) {
            if (result.state == 1) {
                alert('评论成功');
                const comment = {
                    id: result.object,
                    content: text,
                    creator: result.operator.user,
                    like_cnt: 0,
                }
                comment.parent_id = parentId;
                if (type === 'l1') {
                    comment.l2_cnt = 0;
                    comment.poster_rep_cnt = 0;
                }
                const oriCommentCnt = parseInt($('#comment_' + type + '_cnt_' + parentId).text());
                $('#comment_' + type + '_cnt_' + parentId).text(oriCommentCnt + 1);
                processCommentList(parentId, [comment], type, false);
                inputElem.val('');
            } else {
                alert(result.message);
            }
        },
        complete: function () {
            isSendingAjax = false;
        }
    })
}

function switchCommentL1Rank(elem, rankType) {
    const parentId = parseInt($(elem).attr('data-id'));
    if (commentRankType.get(parentId) === rankType)
        return;
    commentHasReachedBottomFlagMap.delete('l1' + parentId);
    commentRankType.set(parentId, 'popular');
    commentQueryTokenMap.delete('l1' + parentId);
    const l1List = l1ListOfPostMap.get(parentId);
    for (let i = 0; i < l1List.length; ++i) {
        commentQueryTokenMap.delete('l2' + l1List[i].id);
        commentHasReachedBottomFlagMap.delete('l2' + l1List[i].id);
    }
    $('#more_comment_l1_btn_' + parentId).text('更多');
    getNewComments(parentId, null, 'l1', rankType, true);
}

function setTop(elem, isCancel) {
    const commentId = parseInt($(elem).attr('data-id'));
    const postId = parseInt($(elem).attr('data-post-id'));
    $.ajax({
        type: 'PATCH',
        url: baseUrl + '/post/top_comment/' + (isCancel ? 'cancel' : 'create'),
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({id: postId, top_comment_id: commentId}),
        success: function (result) {
            if (result.state === 1) {
                alert('设置成功');
                $(elem).text(isCancel ? '置顶' : '取消置顶');
            } else
                alert(result.message);
        }
    });
}