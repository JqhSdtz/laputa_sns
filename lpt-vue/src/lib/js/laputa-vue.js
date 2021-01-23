import lpt from '@/lib/js/laputa'

let checkSignFailCallback;

export function checkSignDirection(el, binding) {
    el.addEventListener('click', (event) => {
        const val = binding.value;
        if (!val) {
            // 不检查是否登录
            return;
        }
        const curOperator = lpt.operatorServ.getCurrent();
        if (curOperator === null || curOperator.user.id === -1) {
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