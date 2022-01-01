import {createRouter, createWebHistory} from 'vue-router';

export function processRouters(param) {
    const router = createRouter({
        history: createWebHistory(),
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

    const titleMap = new Map();
    const titleKeeper = (to, from, next) => {
        titleMap.set(from.fullPath, document.title);
        const nextPath = to.fullPath;
        if (titleMap.has(nextPath)) document.title = titleMap.get(nextPath);
        next();
    }
    return {
        router,
        noCacheList,
        titleKeeper
    }
}