$(document.body).append(getUrlSync(baseStaticUrl + '/comp/category_slc.html'));
const categorySlcTp = template(document.getElementById('categorySlcTp').innerHTML);
let selectedCategoryId = null;
const categoryMap = new Map();

function getData(action, data, showAfterLoad) {
    action({
        data: data,
        success: function (result) {
            const list = result.object;
            for (let i = 0; i < list.length; ++i)
                categoryMap.set(list[i].id, list[i]);
            $('#categoryMenu').html(getCategoryMenu(result.object));
            if (showAfterLoad)
                $('#selectBtn').dropdown('show');
        },
        fail: function (result) {
            alert(result.message);
        }
    });
}

function onCategoryBtnClick(elem) {
    const categoryId = parseInt($(elem).attr('data-id'));
    if ($(elem).attr('data-is-leaf') === 'true') {
        selectedCategoryId = categoryId;
        $('#selectBtn').dropdown('hide');
        const pathList = categoryMap.get(categoryId).path_list;
        let pathStr = '所选目录 ' + getCategoryPathStr(pathList);
        $('#categoryPath').text(pathStr);
    } else
        getData(lpt.categoryServ.getDirectSub, {categoryId: categoryId}, true);
}

$('#selectBtn').click(function () {
    getData(lpt.categoryServ.getRoots, null, true);
});

getData(lpt.categoryServ.getRoots, null, false);

function getCategoryMenu(list) {
    return categorySlcTp({list: list});
}