import lpt from '@/lib/js/laputa';
import global from '@/lib/js/global-state';
import {reactive} from 'vue';

let checkSignFailCallback;

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

lpt.event.on('onCurOperatorChange', operator => {
    // 不能直接替换operator对象，否则会失去响应性
    global.curOperator.user = reactive(operator.user);
});