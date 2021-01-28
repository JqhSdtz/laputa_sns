/**全局状态管理，相当于简易的vuex*/

import {reactive, ref, computed} from 'vue';
import lpt from '@/lib/js/laputa'

const global = {
    isBusy: wrap({
        default: false
    }),
    curOperator: wrap({
        default: lpt.operatorServ.getCurrent()
    }),
    hasSigned: computed(() => global.curOperator.user.id !== -1)
}

function wrap(param) {
    let type, def;
    if (typeof param === 'function') {
        type = param;
        def = type();
    } else if (param.type && typeof param.type === 'function') {
        type = param.type;
        def = param.default || type();
    } else if (typeof param.default !== 'undefined') {
        type = param.default.constructor;
        def = param.default;
    } else {
        return ref();
    }
    return type === Object ? reactive(def) : ref(def);
}

export default global;
