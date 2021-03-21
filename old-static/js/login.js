const submitButton = $('#submitButton');
const userNameFormItem = $('#userNameFormItem');
const passwordFormItem = $('#passwordFormItem');

submitButton.click(function () {
    if (!FormItem.checkFormItems())
        return;
    submitButton.attr('disabled', 'disabled');
    lpt.operatorServ.login({
        data : {
            nick_name: $('#userName').val(),
            password: lpt.md5($('#password').val()),
        },
        success : function() {
            window.location.href = 'post_list.html';
        },
        fail : function(data) {
            FormItem.setWarningText(userNameFormItem, data.message);
            FormItem.showWarning(userNameFormItem);
        },
        error : function(xhr) {
            alert('错误信息  ' + xhr.code + '   请重试');
        },
        complete : function() {
            submitButton.removeAttr('disabled');
        }
    });
});

FormItem.setEmptyWarning(userNameFormItem, '请输入用户名');
FormItem.setEmptyWarning(passwordFormItem, '请输入密码');
FormItem.enableEnterKeySubmit(submitButton);