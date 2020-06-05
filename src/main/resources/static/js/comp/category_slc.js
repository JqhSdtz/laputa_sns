$(document.body).append(getUrlSync(baseStaticUrl + '/comp/category_slc.html'));
const categorySlcTp = template(document.getElementById('categorySlcTp').innerHTML);
let selectedCategoryId = null;
const categoryMap = new Map();

function getData(url, showAfterLoad) {
    $.ajax({
        type: 'GET',
        url: baseUrl + url,
        dataType: 'json',
        success: function (data) {
            if (data.state === 1) {
                const list = data.object;
                for (let i = 0; i < list.length; ++i)
                    categoryMap.set(list[i].id, list[i]);
                $('#categoryMenu').html(getCategoryMenu(data.object));
                if (showAfterLoad)
                    $('#selectBtn').dropdown('show');
            } else
                console.log(data.message);
        }
    });
}

function onCategoryBtnClick(elem) {
    const categoryId = parseInt($(elem).attr('data-id'));
    if ($(elem).attr('data-is-leaf') === 'true'){
        selectedCategoryId = categoryId;
        $('#selectBtn').dropdown('hide');
        const pathList = categoryMap.get(categoryId).path_list;
        let pathStr = '所选目录 ' + getCategoryPathStr(pathList);
        $('#categoryPath').text(pathStr);
    } else
        getData('/category/direct_sub/' + categoryId, true);
}

$('#selectBtn').click(function () {
    getData('/category/roots', true);
});

getData('/category/roots', false);

function getCategoryMenu(list) {
    return categorySlcTp({list: list});
}