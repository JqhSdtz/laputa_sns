$(document.body).append(getUrlSync(baseStaticUrl + '/comp/content.html'));
const contentDropTp = template(document.getElementById('contentDropTp').innerHTML);
let isSendingAjax = false;

function createOrCancelLike(elem, type) {
    const id = parseInt($(elem).attr('data-id'));
    const liked = $(elem).attr('data-liked');
    const likeCnt = parseInt($('#like_cnt_' + type + '_' + id).text());
    if (liked === '0') {//创建
        $.ajax({
            type: 'POST',
            url: baseUrl + '/like/' + type,
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                target_id: id
            }),
            success: function (result) {
                if (result.state === 1) {
                    $(elem).removeClass('icon-heart-empty');
                    $(elem).addClass('icon-heart');
                    $(elem).attr('data-liked', '1');
                    $('#like_cnt_' + type + '_' + id).text(likeCnt + 1);
                } else {
                    alert(result.message);
                }
            }
        });
    } else {//取消
        $.ajax({
            type: 'DELETE',
            url: baseUrl + '/like/' + type,
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                target_id: id
            }),
            success: function (result) {
                if (result.state === 1) {
                    $(elem).removeClass('icon-heart');
                    $(elem).addClass('icon-heart-empty');
                    $(elem).attr('data-liked', '0');
                    $('#like_cnt_' + type + '_' + id).text(likeCnt - 1);
                } else {
                    alert(result.message);
                }
            }
        });
    }
}

function delContent(elem) {
    const id = parseInt($(elem).attr('data-id'));
    const type = $(elem).attr('data-type');
    const creatorId = parseInt($(elem).attr('data-creator-id'));
    let url, targetElem, parentCntElem;
    if (type === 'POST') {
        url = '/post';
        targetElem = $('#post_' + id);
    } else if (type === 'CML1') {
        url = '/comment/l1';
        targetElem = $('#comment_l1_' + id);
        parentCntElem = $('#comment_l1_cnt_' + id);
    } else if (type === 'CML2') {
        url = '/comment/l2';
        targetElem = $('#comment_l2_' + id);
        parentCntElem = $('#comment_l2_cnt_' + id);
    }
    let comment;
    if (creatorId !== getOperator().user_id)
        comment = prompt("请输入操作原因(5-256个字)", "");
    $.ajax({
        type: 'DELETE',
        url: baseUrl + url,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            id: id,
            op_comment: comment
        }),
        success: function (result) {
            if (result.state === 1) {
                alert('删除成功');
                targetElem.hide();
                if (typeof parentCntElem !== 'undefined') {
                    const oriCnt = parseInt(parentCntElem.text());
                    parentCntElem.text(oriCnt - 1);
                }
            } else
                alert(result.message);
        }
    })
}

function showFullText(elem) {
    const fullTextId = parseInt($(elem).attr('data-full-text-id'));
    const id = parseInt($(elem).attr('data-id'));
    $.ajax({
        type: 'GET',
        url: baseUrl + '/post/full_text/' + fullTextId,
        dataType: 'json',
        success: function (result) {
            if (result.state === 1) {
                $(elem).hide();
                $('#post_content_' + id).text(result.object);
            } else
                alert(result.message);
        }
    });
}

function showLikeList(id, type) {
    const href = 'like_list.html?id=' + id + '&type=' + type;
    window.open(href, '_blank');
}