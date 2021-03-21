import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import globalDirectives from '@/lib/js/global/global-directives';
import globalMethods from '@/lib/js/global/global-methods';
import {notification} from 'ant-design-vue';

lpt.event.on('onCurOperatorChange', operator => {
    // 不能直接替换operator对象，否则会失去响应性
    Object.assign(global.states.curOperator, operator);
});
lpt.event.on('pushGlobalBusy', isBusy => {
    // 注册全局繁忙状态响应式属性
    global.events.emit('pushGlobalBusy', isBusy);
});

global.events.on('signUp', (msg) => {
    notification.success({
        message: '注册成功',
        description: msg || '快来发现新世界！',
        duration: 1.5
    });
});

global.events.on('signIn', () => {
    notification.open({
        message: '登录成功',
        duration: 1
    });
});

let checkSignFailCallback;

// 注册权限校验失败的回调
export function registerCheckSignFailCallback(callback) {
    checkSignFailCallback = callback;
}

// 全局的权限校验指令
const checkSignDirection = {
    mounted(el, binding) {
        const value = binding.value;
        if (value.click) {
            el.addEventListener('click', () => {
                if (!lpt.operatorServ.hasSigned()) {
                    if (checkSignFailCallback) {
                        checkSignFailCallback();
                    }
                } else {
                    value.click();
                }
            });
        }
    }
}
globalMethods.checkSign = (callback) => {
    if (!lpt.operatorServ.hasSigned()) {
        if (checkSignFailCallback) {
            checkSignFailCallback();
        }
        return false;
    } else {
        callback && callback();
        return true;
    }
}

const onClickOutsideDirection = {
    mounted(el, binding) {
        if (typeof binding.value !== 'function')
            return;
        document.addEventListener('click', function (event) {
            if (!el.contains(event.target)) {
                binding.value();
            }
        }, true);
    }
}

globalDirectives.push({
    name: 'checkSign',
    handler: checkSignDirection
});
globalDirectives.push({
    name: 'clickOutside',
    handler: onClickOutsideDirection
});