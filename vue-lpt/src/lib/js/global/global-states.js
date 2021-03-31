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
        const res = param.processItem && param.processItem(item);
        if (typeof res !== 'undefined') {
            item = res;
        }
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
    itemManager.get = function (getParam) {
        const id = param.notIntegerId ? getParam.itemId : parseInt(getParam.itemId);
        let res = itemMap.get(id);
        const doRequest = () => {
            if (param.getRequest) {
                return param.getRequest(id).then((item) => {
                    const itemRes = itemManager.add(item);
                    getParam.success && getParam.success(itemRes);
                    return Promise.resolve(itemRes);
                }).catch((error) => {
                    getParam.fail && getParam.fail(error);
                    if (getParam.getPromise) return Promise.reject(error);
                });
            } else {
                getParam.success && getParam.success(res)
                return Promise.resolve(res);
            }
        };
        let promise;
        if (!res) {
            const temp = param.getDefault(id);
            temp.isDefault = true;
            res = itemManager.add(temp);
            promise = doRequest();
        } else {
            if (getParam.filter && !getParam.filter(res) && !getParam.justLocal) {
                promise = doRequest();
            } else {
                getParam.success && getParam.success(res)
                promise = Promise.resolve(res);
            }
        }
        return getParam.getPromise ? promise : res;
    }
    return itemManager;
}

const userManager = initItemManager({
    getDefault(id) {
        return lpt.userServ.getDefaultUser(id);
    }
});

const categoryManager = initItemManager({
    processItem(item) {
        let newItem = item;
        if (item.root)
            newItem = item.root;
        else
            newItem.isFull = false;
        let rights;
        if (item.rights) {
            rights = item.rights;
            newItem.rights = rights;
        } else if (states.curOperator.user.id !== -1) {
            // 没有返回权限信息，则根据permission_map手动判断
            const permissionMap = states.curOperator.permission_map;
            rights = {};
            if (permissionMap) {
                rights.this_level = permissionMap[newItem.id];
                let tmpItem = newItem;
                while (typeof tmpItem.parent_id !== 'undefined') {
                    rights.parent_level = permissionMap[tmpItem.parent_id];
                    if (typeof rights.parent_level !== 'undefined')
                        break;
                    tmpItem = categoryManager.get({
                        itemId: tmpItem.parent_id,
                        justLocal: true
                    });
                    // 如果本地没有已经获取的父目录，则直接退出，不请求服务器
                    if (tmpItem.isDefault)
                        break;
                }
                if (typeof rights.this_level === 'undefined'
                    && typeof rights.parent_level !== 'undefined')
                    rights.this_level = rights.parent_level;
                newItem.rights = rights;
                lpt.categoryServ.setAdminRights(newItem);
            }
        } else {
            rights = {};
            newItem.rights = rights;
        }
        let hasRights = rights.this_level && rights.this_level > 0;
        hasRights = hasRights || (rights.parent_level && rights.parent_level > 0);
        newItem.hasRights = hasRights;
        if (newItem.path_list) {
            newItem.path_list.reverse();
        }
        if (item.sub_list instanceof Array && item.sub_list.length > 0) {
            newItem.sub_list = categoryManager.addList(item.sub_list);
        }
        return newItem;
    },
    getDefault(id) {
        return lpt.categoryServ.getDefaultCategory(id);
    },
    getRequest(id) {
        return lpt.categoryServ.get({
            objectOnly: true,
            throwError: true,
            param: {
                id: id
            }
        });
    }
});

const postManager = initItemManager({
    processItem(item) {
        if (!item.rights)
            item.rights = {};
        if (item.creator) {
            userManager.add(item.creator);
        }
        if (item.category_path) {
            item.category_path.reverse();
        }
        if (typeof item.ori_post === 'object') {
            postManager.add(item.ori_post);
        }
    },
    getDefault(id) {
        return lpt.postServ.getDefaultPost(id);
    },
    getRequest(id) {
        return lpt.postServ.get({
            objectOnly: true,
            param: {
                postId: id
            }
        });
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

const commentL2Manager = initItemManager({
    processItem(item) {
        if (!item.rights)
            item.rights = {};
        if (item.creator) {
            userManager.add(item.creator);
        }
    },
    getDefault(id) {
        return lpt.commentServ.getDefaultCommentL2(id);
    }
});

const noticeManager = initItemManager({
    notIntegerId: true,
    processItem(item) {
        item.id = item.type + ':' + item.content_id;
    },
    getDefault(id) {
        return lpt.noticeServ.getDefaultNotice(id);
    }
});

const states = {
    isBusy: wrap({
        default: false
    }),
    curOperator: wrap({
        default: lpt.operatorServ.getCurrent()
    }),
    style: wrap({
        default: {
            drawerWidth: document.body.clientWidth
        }
    }),
    blog: wrap({
        default: {
            showDrawer: false
        }
    }),
    hasSigned: computed(() => states.curOperator.user.id !== -1),
    postManager,
    commentL1Manager,
    commentL2Manager,
    categoryManager,
    userManager,
    noticeManager,
    pages: {
        index: {
            sortType: wrap({
                default: 'popular'
            })
        }
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
