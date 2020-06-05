const recTp = template(document.getElementById('recTp').innerHTML);
let hasReachedBottom = false;
let curStartId = 0;
let totalLength = 0;
let isSendingAjax = false;

$.ajax({
    type: 'POST',
    url: baseUrl + '/admin_ops/list',
    contentType: 'application/json',
    dataType: 'json',
    data: JSON.stringify({
        query_param: {
            start_id: curStartId,
            query_num: 10
        }
    }),
    success: function (result) {
        if (result.state === 1) {
            setOperator(result.operator);
            initNav(result.operator);
            curStartId = result.object[result.object.length - 1].id;
            processRecList(result.object);
        } else
            alert(result.message);
    }
});

function getNewRec() {
    if (!hasReachedBottom) {
        if (isSendingAjax)
            return;
        isSendingAjax = true;
        $.ajax({
            type: 'POST',
            url: baseUrl + '/admin_ops/list',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                query_param: {
                    start_id: curStartId,
                    query_num: 10
                }
            }),
            success: function (result) {
                if (result.state === 0) {
                    alert(result.message);
                    return;
                }
                if (result.object.length === 0) {
                    hasReachedBottom = true;
                    $('#more-btn').text('没有更多');
                } else
                    curStartId = result.object[result.object.length - 1].id;
                processRecList(result.object);
            },
            complete: function () {
                isSendingAjax = false;
            }
        });
    }
}

function processRecList(list) {
    for (let i = 0; i < list.length; ++i)
        console.log('rec ' + list[i].id);
    totalLength += list.length;
    console.log('-------------------');
    $('#rec-list').append(recTp({list: list}));
}

$('#more-btn').click(function () {
    getNewRec();
});