import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';

lpt.event.on('onCurOperatorChange', operator => {
    // 不能直接替换operator对象，否则会失去响应性
    Object.assign(global.states.curOperator, operator);
});
lpt.event.on('pushGlobalBusy', isBusy => {
    // 注册全局繁忙状态响应式属性
    global.events.emit('pushGlobalBusy', isBusy);
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

export const customDirectionList = [
    {
        name: 'checkSign',
        handler: checkSignDirection
    },
    {
        name: 'clickOutside',
        handler: onClickOutsideDirection
    }
];