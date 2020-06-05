class FormItem {
    constructor(formElem) {
        this.enable = false;
        this.formElem = $(formElem);
        this.warning = $('<p class="warning"></p>');
        this.warningText = null;
        this.inputElem = $(formElem).children('input');
        this.valid = false;
        this.test = null;
        this.formElem.append(this.warning);
        this.warning.hide();
        this.inputElem.focus(FormItem.inputItemOnFocus);
        this.inputElem.blur(FormItem.inputItemOnblur);
    }

    static inputItemOnFocus() {
        const formItem = $(this).parent().prop('formItem');
        if (!formItem.enable)
            return;
        formItem.warning.hide(FormItem.animateTime);
    }

    static inputItemOnblur() {
        const formElem = $(this).parent();
        const formItem = formElem.prop('formItem');
        if (!formItem.enable)
            return;
        const value = $(this).val();
        if (!value || value === "")
            return;
        const testRes = formItem.test(value);
        if (typeof testRes !== "boolean") {//异步验证
            $.when(testRes).fail(function () {
                formItem.warning.text(formItem.warningText);
                formItem.warning.show(FormItem.animateTime);
                formItem.valid = false;
            });
            $.when(testRes).done(function () {
                formItem.valid = true;
            });
        } else {//同步验证
            if (!testRes) {
                formItem.warning.text(formItem.warningText);
                formItem.warning.show(FormItem.animateTime);
                formItem.valid = false;
                return;
            } else {
                formItem.valid = true;
            }
        }
    }

    /**formItem为jQuery封装的dom元素，test为测试函数
     * 若test函数同步，则返回true或false，若异步则返回promise对象
     * 在异步函数中，若验证通过则resolve，否则reject*/
    static setTest(formElem, test) {
        const formItem = formElem.prop('formItem');
        if (!formItem.enable)
            formItem.enable = true;
        formItem.test = test;
    }

    static setWarningText(formElem, warningText) {
        formElem.prop('formItem').warningText = warningText;
    }

    static equals(formElem1, formElem2) {
        let value1 = formElem1.prop('formItem').inputElem.val();
        let value2 = formElem2.prop('formItem').inputElem.val();
        if (value1 === value2)
            return true;
        else
            return false;
    }

    static setEmptyWarning(formElem, warningText) {
        formElem.prop('emptyWarningText', warningText);
    }

    static showWarning(formElem) {
        const formItem = formElem.prop('formItem');
        formItem.warning.text(formItem.warningText);
        formItem.warning.show(FormItem.animateTime);
    }

    static enableEnterKeySubmit(submitButton) {
        for (let i = 0; i < FormItem.formItemList.length; ++i) {
            let formItem = FormItem.formItemList[i];
            formItem.inputElem.keydown(function (event) {
                if (event.keyCode === 13) {
                    $(this).blur();
                    submitButton.click();
                }
            });
        }
    }

    /**表单提交前需调用此函数执行检查
     *表单中的必填项需要加required属性*/
    static checkFormItems() {
        let emptyCnt = 0;
        for (let i = 0; i < FormItem.formItemList.length; ++i) {
            let formItem = FormItem.formItemList[i];
            if (!formItem.enable)
                continue;
            let value = formItem.inputElem.val();
            if (!value || value === "") {
                if (!formItem.formElem.attr('required')) {
                    ++emptyCnt;
                    continue;
                }
                formItem.warning.text(formItem.formElem.prop('emptyWarningText'));
                formItem.warning.show(FormItem.animateTime);
                return false;
            }
            if (!formItem.valid)
                return false;
        }
        if (emptyCnt === FormItem.formItemList.length) {
            alert("表单为空！");
            return false;
        }
        return true;
    }
}

FormItem.animateTime = 250;

/*构造表单项*/
const formElemList = $('.formItem');
FormItem.formItemList = new Array();
for (let i = 0; i < formElemList.length; ++i) {
    let formItem = new FormItem(formElemList[i]);
    $(formElemList[i]).prop('formItem', formItem);
    FormItem.formItemList.push(formItem);
}
$('.warning').css({'color': 'red'});


