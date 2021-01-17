import {createRouter, createWebHashHistory} from 'vue-router';
import Index from '@/pages/Index';
import News from '@/pages/News';
import Community from '@/pages/Community';
import Mine from '@/pages/Mine';

export default createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '/',
            redirect: '/index'
        },
        {
            path: '/index',
            name: 'index',
            component: Index
        },
        {
            path: '/news',
            name: 'news',
            component: News
        },
        {
            path: '/community',
            name: 'community',
            component: Community
        },
        {
            path: '/mine',
            name: 'mine',
            component: Mine
        }
    ]
})
