import {createRouter, createWebHashHistory} from 'vue-router';
import Home from '@/components/home/Home'
import Index from '@/pages/inside/index/Index';
import News from '@/pages/inside/news/News';
import Community from '@/pages/inside/community/Community';
import Mine from '@/pages/inside/mine/Mine';
import ModUserInfo from '@/pages/outside/user/ModUserInfo';
import SignIn from '@/pages/outside/sign/SignIn';
import SignUp from '@/pages/outside/sign/SignUp';
import PostDetail from '@/components/post/post_detail/PostDetail';
import Publish from '@/pages/outside/publish/Publish';
import UserHomePage from '@/pages/outside/user/UserHomePage';
import FollowersList from '@/pages/outside/user/user_list/FollowersList';
import FollowingList from '@/pages/outside/user/user_list/FollowingList';
import NoticeList from '@/pages/outside/notice/NoticeList';
import CommentLikeList from '@/components/post/post_detail/comment_detail/CommentLikeList';
import SearchIndex from '@/pages/outside/search/SearchIndex';
import CategoryDetail from '@/pages/outside/category/detail/CategoryDetail';
import CategorySetting from '@/pages/outside/category/CategorySetting';
import ModCategoryInfo from '@/pages/outside/category/ModCategoryInfo';
import PermissionList from '@/pages/outside/user/PermissionList';

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
        path: '/mod_user_info',
        name: 'modUserInfo',
        component: ModUserInfo
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
    },
    {
        path: '/post_detail/:postId',
        name: 'postDetail',
        component: PostDetail,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/publish',
        name: 'publish',
        component: Publish
    },
    {
        path: '/user_home_page/:userId',
        name: 'userHomePage',
        component: UserHomePage,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/followers_list/:userId',
        name: 'followersList',
        component: FollowersList,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/following_list/:userId',
        name: 'followingList',
        component: FollowingList,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/notice_list',
        name: 'noticeList',
        component: NoticeList
    },
    {
        path: '/comment_like_list/:type/:commentId',
        name: 'commentLikeList',
        component: CommentLikeList,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/search_index',
        name: 'searchIndex',
        component: SearchIndex,
        meta: {
            noCache: true
        }
    },
    {
        path: '/category_detail/:categoryId',
        name: 'categoryDetail',
        component: CategoryDetail,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/category_setting/:categoryId',
        name: 'categorySetting',
        component: CategorySetting,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/mod_category_info/:categoryId',
        name: 'modCategoryInfo',
        component: ModCategoryInfo,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/permission_list/:userId',
        name: 'permissionList',
        component: PermissionList,
        props: true,
        meta: {
            noCache: true
        }
    }
];
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