import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global/global-state';
import {reactive} from 'vue';

export function initLaputa() {
    lpt.event.on('onCurOperatorChange', operator => {
        // 不能直接替换operator对象，否则会失去响应性
        global.curOperator.user = reactive(operator.user);
    });

    lpt.event.on('globalBusyChange', isBusy => {
        // 注册全局繁忙状态响应式属性
        global.isBusy.value = isBusy;
    });
}

let checkSignFailCallback;

// 注册权限校验失败的回调
export function registerCheckSignFailCallback(callback) {
    checkSignFailCallback = callback;
}

// 全局的权限校验指令
const checkSignDirection = function (el, binding) {
    el.addEventListener('click', (event) => {
        const val = binding.value;
        if (!val) {
            // 不检查是否登录
            return;
        }
        if (!lpt.operatorServ.hasSigned()) {
            if (checkSignFailCallback) {
                checkSignFailCallback();
            }
            event.preventDefault();
        }
    });
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
