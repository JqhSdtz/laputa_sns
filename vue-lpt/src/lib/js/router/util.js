import {createRouter, createWebHashHistory} from 'vue-router';
import global from '@/lib/js/global';

export function processRouters(param) {
    const router = createRouter({
        history: createWebHashHistory(),
        routes: param.routers
    });
    const noCacheList = [];
    function doProcess(_routers) {
        _routers.forEach(router => {
            if (router.meta) {
                if (router.meta.noCache && router.component) {
                    noCacheList.push(router.component.name);
                }
            }
            if (router.children) {
                doProcess(router.children);
            }
        });
    }
    doProcess(param.routers);
    let curHomeInsidePath = param.homeIndexPath;
    const homePath = param.homePath;
    const resolvedSet = new Set();
    router.beforeEach((to, from, next) => {
        if (to.meta && to.meta.checkSign && !global.methods.checkSign()) {
            return;
        }
        if (resolvedSet.has(from)) {
            resolvedSet.delete(from);
            next();
            return;
        }
        if (from.path === '/') {
            next();
            return;
        }
        const fromIdxOfHome = from.path.indexOf(homePath);
        const fromHome = fromIdxOfHome > -1 && fromIdxOfHome < 2;
        const toIdxOfHome = to.path.indexOf(homePath);
        const toHome = toIdxOfHome > -1 && toIdxOfHome < 2;
        if (fromHome && !toHome) {
            // 从home里出来时记录home中的路径
            curHomeInsidePath = from.path;
        } else if (!fromHome && toHome) {
            // 从home外回来时直接跳转到对应的路径
            resolvedSet.add(from);
            next({path: curHomeInsidePath});
            return;
        }
        next();
    });
    return {
        router,
        noCacheList
    }
}