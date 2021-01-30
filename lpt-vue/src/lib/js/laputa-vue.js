import lpt from '@/lib/js/laputa';
import global from '@/lib/js/global-state';
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

// 全局的权限校验指令
export function checkSignDirection(el, binding) {
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

// 注册权限校验失败的回调
export function registerCheckSignFailCallback(callback) {
    checkSignFailCallback = callback;
}

export const testDirection = {
    beforeMount(el, binding, vNode) {
        console.log(el);
        console.log(binding);
        console.log(vNode);
    }
}

