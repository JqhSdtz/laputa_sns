const submitButton = $('#submitButton');
const userNameFormItem = $('#userNameFormItem');
const passwordFormItem = $('#passwordFormItem');

submitButton.click(function () {
    if (!FormItem.checkFormItems())
        return;
    submitButton.attr('disabled', 'disabled');
    const param = {
        nick_name: $('#userName').val(),
        password: md5($('#password').val()),
    };
    $.ajax({
        type: 'POST',
        url: baseUrl + '/operator/login',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(param),
        success: function (data) {
            if (data.state === 1) {
                window.location.href = 'post_list.html';
            } else {
                FormItem.setWarningText(userNameFormItem, data.message);
                FormItem.showWarning(userNameFormItem);
            }
        },
        error: function (xhr) {
            alert('错误信息  ' + xhr.code + '   请重试');
        },
        complete: function () {
            submitButton.removeAttr('disabled');
        }
    });
});

FormItem.setEmptyWarning(userNameFormItem, '请输入用户名');
FormItem.setEmptyWarning(passwordFormItem, '请输入密码');
FormItem.enableEnterKeySubmit(submitButton);