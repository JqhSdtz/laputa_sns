/**全局状态管理，相当于简易的vuex*/

import {reactive, ref, computed} from 'vue';
import lpt from '@/lib/js/laputa/laputa';

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

function initItemManager(type) {
    const itemMap = new Map();
    const itemManager = {};
    itemManager.getItemMap = function () {
        return itemMap;
    }
    itemManager.add = function (item) {
        if (itemMap.has(item.id)) {
            // 因为直接通过map的set方法设置对象会直接覆盖，不会引起响应式变化
            // 所以这里需要逐一赋值
            const original = itemMap.get(item.id);
            Object.assign(original, item);
            return original;
        } else {
            const _item = reactive(item);
            itemMap.set(item.id, _item);
            return _item;
        }
    }
    itemManager.addList = function (itemList, keepUnRef = false) {
        // 可以选择不将列表中原有对象转化为响应式对象
        itemList.map(item => {
            return keepUnRef ? item : itemManager.add(item);
        });
        return itemList;
    }
    itemManager.get = function (itemId) {
        const id = parseInt(itemId);
        let res = itemMap.get(id);
        if (!res) {
            let temp;
            if (type === lpt.contentType.post) {
                temp = lpt.postServ.getDefaultPost(id);
            } else if (type === lpt.contentType.commentL1) {
                temp = lpt.commentServ.getDefaultCommentL1(id);
            } else if (type === lpt.contentType.commentL2) {
                temp = lpt.commentServ.getDefaultCommentL2(id);
            }
            res = itemManager.add(temp);
        }
        return res;
    }
    return itemManager;
}

const states = {
    isBusy: wrap({
        default: false
    }),
    curOperator: wrap({
        default: lpt.operatorServ.getCurrent()
    }),
    hasSigned: computed(() => states.curOperator.user.id !== -1),
    postManager: initItemManager(lpt.contentType.post),
    commentL1Manager: initItemManager(lpt.contentType.commentL1),
    pages: {
        index: {
            sortType: wrap({
                default: 'popular'
            })
        }
    }
}

export default states;
