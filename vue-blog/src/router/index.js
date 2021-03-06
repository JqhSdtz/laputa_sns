import {createRouter, createWebHashHistory} from 'vue-router';
import Home from '@/components/Home';
import Index from '@/components/Index';

const routers = [
        {
            path: '/',
            redirect: '/home/index'
        },
        {
            path: '/home',
            name: 'home',
            component: Home,
            children: [
                {
                    path: 'index',
                    component: Index
                }
            ]
        }
    ]
;
const router = createRouter({
    history: createWebHashHistory(),
    routes: routers
});

const _noCacheList = [];

function processRouters(_routers) {
    _routers.forEach(router => {
        if (router.meta) {
            if (router.meta.noCache && router.component) {
                _noCacheList.push(router.component.name);
            }
        }
        if (router.children) {
            processRouters(router.children);
        }
    });
}

processRouters(routers);


export const noCacheList = _noCacheList;

let curHomeInsidePath = '/home/index';
const resolvedSet = new Set();
router.beforeEach((to, from, next) => {
    if (resolvedSet.has(from)) {
        resolvedSet.delete(from);
        next();
        return;
    }
    if (from.path === '/') {
        next();
        return;
    }
    const fromIdxOfHome = from.path.indexOf('home');
    const fromHome = fromIdxOfHome > -1 && fromIdxOfHome < 2;
    const toIdxOfHome = to.path.indexOf('home');
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

export default router;