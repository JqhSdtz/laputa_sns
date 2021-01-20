import {createRouter, createWebHashHistory} from 'vue-router';
import Home from '@/components/home/Home'
import Index from '@/pages/inside/Index';
import News from '@/pages/inside/News';
import Community from '@/pages/inside/Community';
import Mine from '@/pages/inside/Mine';
import SignIn from '@/pages/outside/SignIn';
import SignUp from '@/pages/outside/SignUp';

export default createRouter({
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
})
