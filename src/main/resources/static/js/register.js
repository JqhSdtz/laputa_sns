const userNamePattern = /^[^@#$<>()\[\]\\\.,;:\s"]{2,12}$/;
const submitButton = $('#submitButton');
const userNameFormItem = $('#userNameFormItem');
const passwordFormItem = $('#passwordFormItem');
const confirmPwdFormItem = $('#confirmPwdFormItem');
let checkIdDeferred;

FormItem.setTest(userNameFormItem, function (value) {
    if (!userNamePattern.test(value)) {
        FormItem.setWarningText(userNameFormItem, '用户名长度应在2-12之间，且不能包含@#$<>()\[\]\\\.,;:\"以及空格');
        return false;
    } else {
        const deferred = $.Deferred();
        checkIdDeferred = $.get(baseUrl + '/user/check_name/' + $('#userName').val(), function (data) {
            if (data.state === 0) {
                FormItem.setWarningText(userNameFormItem, data.message);
                deferred.reject();
            } else {
                deferred.resolve();
            }
        }, 'json');
        return deferred.promise();
    }
});

FormItem.setTest(passwordFormItem, function (value) {
    FormItem.setWarningText(passwordFormItem, '密码长度应大于6');
    return value.length >= 6;
});

FormItem.setTest(confirmPwdFormItem, function () {
    if (!FormItem.equals(passwordFormItem, confirmPwdFormItem)) {
        FormItem.setWarningText(confirmPwdFormItem, '两次输入密码不一致');
        return false;
    }
    return true;
});

submitButton.click(function () {//提交表单前验证
    if (!FormItem.checkFormItems())
        return;
    submitButton.attr('disabled', 'disabled');
    $.when(checkIdDeferred).done(function () {
        $.ajax({
            type: 'POST',
            url: baseUrl + '/operator/register',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify({
                nick_name: $('#userName').val(),
                password: md5($('#password').val()),
            }),
            success: function (data) {
                if (data.state === 1) {
                    window.location.href = 'post_list.html';
                } else {
                    alert(data.message);
                }
            },
            error: function (xhr) {
                alert('错误信息 ' + xhr.code + ' 请重试');
            },
            complete: function () {
                submitButton.removeAttr('disabled');
            }
        });
    });
});

FormItem.setEmptyWarning(userNameFormItem, '请输入用户名');
FormItem.setEmptyWarning(passwordFormItem, '请输入密码');
FormItem.setEmptyWarning(confirmPwdFormItem, '请输入再次密码');
FormItem.enableEnterKeySubmit(submitButton);