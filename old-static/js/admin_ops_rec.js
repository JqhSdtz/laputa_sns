const recTp = template(document.getElementById('recTp').innerHTML);

lpt.adminOpsServ.queryAdminOps({
    success: function(result) {
        initNav(result.operator);
        processRecList(result.object);
    },
    fail: function(result) {
        alert(result.message);
    }
});

function getNewRec() {
    if (!lpt.adminOpsServ.querior.hasReachedBottom) {
        lpt.adminOpsServ.queryAdminOps({
            success: function(result) {
                if (result.object.length === 0) {
                    $('#more-btn').text('没有更多');
                }
                processRecList(result.object);
            },
            fail: function(result) {
                alert(result.message);
            }
        });
    }
}

function processRecList(list) {
    for (let i = 0; i < list.length; ++i)
        console.log('rec ' + list[i].id);
    console.log('-------------------');
    $('#rec-list').append(recTp({list: list}));
}

$('#more-btn').click(function () {
    getNewRec();
});