const lptMode = 2;
const baseUrl = lptMode === 1 ? 'http://localhost:8080/lpt' : 'https://lpt.ytumore.cn';
const baseStaticUrl = lptMode === 1 ? baseUrl : baseUrl + '/static';

function aesEncrypt(key, data) {
    key = CryptoJS.enc.Utf8.parse(key);
    const res = CryptoJS.AES.encrypt(data, key, {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
    }).toString();
    return res;
}

function md5(data) {
    return CryptoJS.MD5(data).toString();
}

function base64UrlEncode(data) {
    return CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(data))
        .replace(/\+/g, '-').replace(/\//g, '_');
}

Date.prototype.format = function (format) {
    let o = {
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "h+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
        "q+": Math.floor((this.getMonth() + 3) / 3),
        "S": this.getMilliseconds()
    }
    if (/(y+)/.test(format)) {
        format = format.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    }
    for (let k in o) {
        if (new RegExp("(" + k + ")").test(format)) {
            format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length));
        }
    }
    return format;
}

/**
 date 为long类型
 pattern 为格式化参数
 */
function getFormatDate(date, pattern) {
    if (date == undefined) {
        date = new Date();
    } else if (!(date instanceof Date)) {
        date = new Date(date);
    }
    if (pattern == undefined) {
        pattern = "yyyy-MM-dd hh:mm:ss";
    }
    return date.format(pattern);
}

function getCategoryPathStr(pathList) {
    let pathStr = '';
    for (let i = pathList.length - 1; i >= 0; --i)
        pathStr += pathList[i].name + '\\';
    return pathStr;
}

function getParamOfUrl(name, href) {
    let query;
    if (typeof href !== 'undefined')
        query = href.split('?').length > 1 ? href.split('?')[1] : '';
    else
        query = window.location.search.substring(1);
    const vars = query.split('&');
    for (let i = 0; i < vars.length; i++) {
        let pair = vars[i].split('=');
        if (pair[0] == name) {
            return pair[1];
        }
    }
    return '';
}

function toggleElem() {
    doToggle(arguments[0]);
    for (let i = 1; i < arguments.length; ++i)
        doToggle(arguments[i], true);
}

function doToggle(elem_id, hide) {
    const elem = $('#' + elem_id);
    const shown = elem.attr('data-shown');
    if (hide || (typeof shown !== 'undefined' && shown === '1')) {
        elem.hide();
        elem.attr('data-shown', '0');
    } else {
        elem.show();
        elem.attr('data-shown', '1');
    }
}

function getUserAvatarUrl(user) {
    if (typeof user.raw_avatar === 'undefined')
        return baseStaticUrl + '/img/def_ava.jpg';
    return 'https://img.ytumore.cn/' + user.raw_avatar + '!ava.small/clip/50x50a0a0/gravity/center'
}

function getPostThumbUrl(rawUrl) {
    return 'https://img.ytumore.cn/' + rawUrl + '!/both/50x50';
}

function showFullImg(rawUrl) {
    $('#modal-img').attr('src', 'https://img.ytumore.cn/' + rawUrl);
    $('#img-modal').modal('show');
}

function getUrlSync(url) {
    let res;
    $.ajax({
        type: 'GET', async: false, url: url, success: function (data) {
            res = data;
        }
    });
    return res;
}

function setOperator(_operator) {
    operator = _operator;
    localStorage.setItem('lpt_operator', JSON.stringify(_operator));
}

function getOperator() {
    if (typeof operator !== 'undefined')
        return operator;
    const st = localStorage.getItem('lpt_operator');
    return st === null ? null : JSON.parse(st);
}

function showUser(id) {
    window.open('user_info.html?user_id=' + id, '_blank');
}

function showCategory(categoryId) {
    window.open('category_idx.html?root_id=' + categoryId, '_blank');
}

function isAdmin(op) {
    if (typeof op === 'undefined')
        op = getOperator();
    const tmp = op.permission_map;
    return typeof tmp !== 'undefined' && Object.keys(tmp).length !== 0;
}

let operator;
