import {createRouter, createWebHashHistory} from 'vue-router';
import Home from '@/components/home/Home'
import Index from '@/pages/inside/Index';
import News from '@/pages/inside/News';
import Community from '@/pages/inside/Community';
import Mine from '@/pages/inside/Mine';
import SignIn from '@/pages/outside/SignIn';
import SignUp from '@/pages/outside/SignUp';

const router =  createRouter({
    history: createWebHashHistory(),
    routes: [
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
                },
                {
                    path: 'news',
                    component: News
                },
                {
                    path: 'community',
                    component: Community
                },
                {
                    path: 'mine',
                    component: Mine
                }
            ]
        },
        {
            path: '/sign_in',
            name: 'signIn',
            component: SignIn
        },
        {
            path: '/sign_up',
            name: 'signUp',
            component: SignUp
        }
    ]
});

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
})

export default router;