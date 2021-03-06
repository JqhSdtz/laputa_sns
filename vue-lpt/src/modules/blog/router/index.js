import Home from '@/modules/blog/pages/home/Home'
import {processRouters} from '@/lib/js/router/util';


const routers = [
    {
        path: '/',
        redirect: '/home'
    },
    {
        path: '/home',
        name: 'home',
        component: Home
    }
];
const {router, noCacheList} = processRouters({
    routers,
    homePath: 'home',
    homeIndexPath: '/home/index'
});

export {
    router as default,
    noCacheList
};