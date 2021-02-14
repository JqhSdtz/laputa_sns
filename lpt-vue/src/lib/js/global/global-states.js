/**全局状态管理，相当于简易的vuex*/

import {reactive, ref, computed} from 'vue';
import lpt from '@/lib/js/laputa/laputa';
import events from './global-events';

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

function initItemManager(param) {
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
            param.processItem && param.processItem(item);
            // const keySet = new Set();
            // // 获取原有对象和现有对象的所有key
            // for (let key in original)
            //     keySet.add(key);
            // for (let key in item)
            //     keySet.add(key);
            // keySet.forEach(key => {
            //    if (Object.prototype.hasOwnProperty.call(item, key)) {
            //        // eslint禁止直接使用hasOwnProperty
            //        // 新对象有的，要给原对象加上去
            //        original[key] = item[key];
            //    } else {
            //        // 新对象没有的，要从原对象中去掉
            //        delete original[key];
            //    }
            // });
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
            const temp = param.getDefault(id);
            res = itemManager.add(temp);
        }
        return res;
    }
    return itemManager;
}

const userManager = initItemManager({
    getDefault(id) {
        return lpt.userServ.getDefaultUser(id);
    }
});

const postManager = initItemManager({
    processItem(item) {
        if (!item.rights)
            item.rights = {};
        if (item.creator) {
            userManager.add(item.creator);
        }
    },
    getDefault(id) {
        return lpt.postServ.getDefaultPost(id);
    }
});

const commentL1Manager = initItemManager({
    processItem(item) {
        if (!item.rights)
            item.rights = {};
        if (item.creator) {
            userManager.add(item.creator);
        }
    },
    getDefault(id) {
        return lpt.commentServ.getDefaultCommentL1(id);
    }
});

const states = {
    isBusy: wrap({
        default: false
    }),
    curOperator: wrap({
        default: lpt.operatorServ.getCurrent()
    }),
    hasSigned: computed(() => states.curOperator.user.id !== -1),
    postManager: postManager,
    commentL1Manager: commentL1Manager,
    userManager: userManager,
    pages: {
        index: {
            sortType: wrap({
                default: 'popular'
            })
        }
    },
    prompt: {
        show: wrap({
            default: false
        }),
        onConfirm: wrap({
            type: Function
        })
    }
}

export default states;

function initGlobalBusyHandler() {
    let globalBusyCount = 0;
    events.on('pushGlobalBusy', (param) => {
        // 之前同时执行的请求数和当前请求数符号不同
        // 即从0到正或从正到0，或异常时从负到正、从正到负
        // 发起全局繁忙状态改变
        const oriBusyCount = globalBusyCount;
        // promise是线程安全的，通过事件循环实现，无需考虑++操作的原子性
        globalBusyCount += param.isBusy ? 1 : -1;
        if (Math.sign(oriBusyCount) !== Math.sign(globalBusyCount)
            && !param.ignoreGlobalBusyChange) {
            states.isBusy.value = param.isBusy;
        }
    });
}
initGlobalBusyHandler();
