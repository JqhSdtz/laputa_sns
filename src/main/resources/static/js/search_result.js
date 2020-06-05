
const searchType = getParamOfUrl('type');
const words = getParamOfUrl('words');
const mode = getParamOfUrl('mode');
$.ajax({
    type: 'GET',
    url: baseUrl + '/search/' + searchType + '/' + words + '/' + mode,
    dataType: 'json',
    success: function (result) {
        if (result.state === 0) {
            alert(result.message);
            return;
        }
        $(document.body).html(JSON.stringify(result));
    }
});