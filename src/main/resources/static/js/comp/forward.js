$(document.body).append(getUrlSync(baseStaticUrl + '/comp/forward.html'));

const forwardTp = template(document.getElementById('forwardTp').innerHTML);
const forwardOfPostTp = template(document.getElementById('forwardOfPostTp').innerHTML);

const forwardListOfPostMap = new Map();

function forwardClear() {
    lpt.forwardServ.clearQueriors();
}

function processForwardList(id, list) {
    for (let i = 0; i < list.length; ++i)
        console.log('fw ' + list[i].id);
    console.log('-------------------');
    const html = forwardTp({list: list});
    $('#forward_list_' + id).append(html);
}

function readMoreForward(elem) {
    const parentId = parseInt($(elem).attr('data-id'));
    getNewForward(parentId, null);
}

function showPostForward(elem) {
    const parentId = parseInt($(elem).attr('data-id'));
    const shown = $(elem).attr('data-shown');
    const gotten = $(elem).attr('data-gotten');
    if ($('forward_cnt_' + parentId).text() !== '0' && shown === '0' && gotten !== '1')
        getNewForward(parentId, elem);
    toggleElem('forward_of_post_' + parentId, 'comment_l1_of_post_' + parentId);
}

function getNewForward(parentId, elem) {
    if (!lpt.forwardServ.getQuerior(parentId).hasReachedBottom) {
        lpt.forwardServ.query({
            data: {
                sup_id: parentId
            },
            success: function (result) {
                const forwardList = result.object;
                const forwardOfPostList = forwardListOfPostMap.has(parentId) ? forwardListOfPostMap.get(parentId) : new Array();
                for (let i = 0; i < forwardList.length; ++i)
                    forwardOfPostList.push(forwardList[i]);
                forwardListOfPostMap.set(parentId, forwardOfPostList);
                if (result.object.length === 0) {
                    $('#more_forward_btn_' + parentId).text('没有更多');
                }
                if (elem !== null)
                    $(elem).attr('data-gotten', '1');
                processForwardList(parentId, result.object);
            },
            fail: function (result) {
                alert(result.message);
            }
        });
    }
}

function sendForward(elem, type) {
    const parentId = parseInt($(elem).attr('data-id'));
    const inputElem = $('#forward_text_' + parentId);
    const text = inputElem.val();
    if (text.length === 0) {
        alert('请输入内容');
        return;
    } else if (text.length > 249) {
        alert('转发评论内容太长');
        return;
    }
    lpt.forwardServ.create({
        data: {
            sup_id: parentId,
            content: text
        },
        success: function (result) {
            alert('转发成功');
            const forward = {
                id: result.object,
                creator: result.operator.user,
            }
            forward.parent_id = parentId;
            const oriForwardCnt = parseInt($('#forward_cnt_' + parentId).text());
            $('#forward_cnt_' + parentId).text(oriForwardCnt + 1);
            processForwardList(parentId, [forward]);
            inputElem.val('');
        },
        fail: function (result) {
            alert(result.message);
        }
    });
}