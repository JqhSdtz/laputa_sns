/**全局状态管理，相当于简易的vuex*/

import {reactive, ref, computed} from 'vue';
import lpt from '@/lib/js/laputa/laputa'

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

const global = {
    isBusy: wrap({
        default: false
    }),
    curOperator: wrap({
        default: lpt.operatorServ.getCurrent()
    }),
    hasSigned: computed(() => global.curOperator.user.id !== -1),
    postManager: initPostManager()
}

function initPostManager() {
    const postMap = new Map();
    const postManager = {};
    postManager.add = function (post) {
        const _post = reactive(post);
        postMap.set(post.id, _post);
        return _post;
    }
    postManager.addList = function (postList, keepUnRef = false) {
        // 可以选择不将列表中原有对象转化为响应式对象
        postList.map(post => {
           return keepUnRef ? post : postManager.add(post);
        });
        return postList;
    }
    postManager.get = function (post_id) {
        return postMap.get(parseInt(post_id));
    }
    return postManager;
}

export default global;
