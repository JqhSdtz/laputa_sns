const searchType = getParamOfUrl('type');
const words = getParamOfUrl('words');
const mode = getParamOfUrl('mode');
lpt.searchServ.search({
    data : {
        searchType: searchType,
        words: words,
        mode: mode,
    },
    fail: function (result) {
        alert(result.message);
    },
    finish: function (result) {
        $(document.body).html(JSON.stringify(result));
    }
});