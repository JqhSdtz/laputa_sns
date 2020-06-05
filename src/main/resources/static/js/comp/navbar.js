$('#navbar').append(getUrlSync(baseStaticUrl + '/comp/navbar.html'));

function initNav(operator) {
    if (operator.user_id === -1) {
        $('#userName').html('游客').attr('href', 'login.html');
        $('#userDrop1').html('登录').attr('href', 'login.html');
        $('#userDrop2').html('注册').attr('href', 'register.html');
        $('#userDrop3').show().html('');
    } else {
        $('#userName').html(operator.user.nick_name).attr('href', 'login.html');
        $('#userDrop1').html('个人中心').attr('href', 'user_info.html?user_id=' + operator.user_id);
        $('#userDrop2').html('修改信息').attr('href', 'mod_user.html');
        $('#userDrop3').show().html('注销').click(function () {
            $.ajax({
                type: 'POST',
                url: baseUrl + '/operator/logout',
                dataType: 'json',
                success: function (result) {
                    if (result.state === 1) {
                        setOperator(result.operator)
                        $('#userName').html('游客').attr('href', 'login.html');
                        $('#userDrop1').html('登录').attr('href', 'login.html');
                        $('#userDrop2').html('注册').attr('href', 'register.html');
                    } else
                        alert(result.message);
                }
            });
        });
        if (typeof operator.unread_news_cnt !== 'undefined' && operator.unread_news_cnt !== 0)
            $('#unread_news_cnt').html(operator.unread_news_cnt).show();
        if (typeof operator.unread_notice_cnt !== 'undefined' && operator.unread_notice_cnt !== 0)
            $('#unread_notice_cnt').html(operator.unread_notice_cnt).show();
        if (isAdmin(operator))
            $('#admin_ops_rec_bar').show();
    }
}

const hrefA = window.location.pathname.split('/');
const pageName = hrefA[hrefA.length - 1];
const navItems = $('#nav-items').children();
for (let i = 0; i < navItems.length; ++i) {
    if ($(navItems[i]).children('a').attr('href') === pageName)
        $(navItems[i]).addClass('active');
}

let searchType = 'post';

function setSearchType(elem) {
    const typeStr = $(elem).attr('data-search-type');
    searchType = typeStr;
    $('#search-type-btn').text($(elem).text());
}

function submitSearch() {
    const words = $('#search-words').val();
    if (!words || !words.length) {
        alert('请输入搜索内容');
        return;
    }
    const mode = $('#search-mode').prop("checked") ? 'bool' : 'natrl';
    window.open('search_result.html?type=' + searchType + '&words=' + words + '&mode=' + mode, '_blank');
}