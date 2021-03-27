import {processRouters} from '@/lib/js/router/util';
import SignIn from '@/modules/lpt/pages/outside/sign/SignIn';
import SignUp from '@/modules/lpt/pages/outside/sign/SignUp';
import Mine from "@/modules/lpt/pages/inside/mine/Mine";
import ModUserInfo from "@/modules/lpt/pages/outside/user/ModUserInfo";
import UserHomePage from "@/modules/lpt/pages/outside/user/UserHomePage";
import PostDetail from "@/components/post/post_detail/PostDetail";
import Publish from "@/modules/lpt/pages/outside/publish/Publish";
import FollowersList from "@/modules/lpt/pages/outside/user/user_list/FollowersList";
import FollowingList from "@/modules/lpt/pages/outside/user/user_list/FollowingList";
import NoticeList from "@/modules/lpt/pages/outside/notice/NoticeList";
import CommentLikeList from "@/components/post/post_detail/comment_detail/CommentLikeList";
import SearchIndex from "@/modules/lpt/pages/outside/search/SearchIndex";
import CategoryDetail from "@/modules/lpt/pages/outside/category/detail/CategoryDetail";
import CategorySetting from "@/modules/lpt/pages/outside/category/CategorySetting";
import ModCategoryInfo from "@/modules/lpt/pages/outside/category/ModCategoryInfo";
import PermissionList from "@/modules/lpt/pages/outside/user/PermissionList";
import CategoryAdminList from "@/modules/lpt/pages/outside/category/CategoryAdminList";
import DruidStat from "@/modules/lpt/pages/outside/DruidStat";
import Index from "@/modules/lpt/pages/inside/index/Index";
import global from "@/lib/js/global";
import pathToRegexp from 'path-to-regexp';

const drawerRouters = [
    {
        path: '/home/mine',
        name: 'mine',
        component: Mine
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
        component: Publish,
        meta: {
            checkSign: true
        }
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
        component: NoticeList,
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
    },
    {
        path: '/category_admin_list/:categoryId',
        name: 'categoryAdminList',
        component: CategoryAdminList,
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/druid_stat',
        name: 'druidStat',
        component: DruidStat
    }
];
const routers = [
    {
        path: '/',
        redirect: '/blog/index'
    },
    {
        path: '/blog/index',
        component: Index
    },
    {
        path: '/blog/post_detail/:postId',
        name: 'blogPostDetail',
        component: PostDetail,
        props: true,
        meta: {
            noCache: true
        }
    },
];

// 以下循环创建路由项，为每一个主页面和抽屉页面的组合都创建一个路由
// 以此实现主页面和抽屉页面分别路由
const len = routers.length;
for (let i = 0; i < len; ++i) {
    const route = routers[i];
    if (route.path === '/')
        continue;
    drawerRouters.forEach((dRoute) => {
        const newRoute = {
            path: route.path + dRoute.path,
            name: (route.name || '') + '_' +(dRoute.name || ''),
            props: {
                default: route.props,
                leftDrawer: dRoute.props
            },
            meta: {
                ...route.meta,
                ...dRoute.meta,
                mainPath: route.path,
                drawerPath: dRoute.path,
                isDrawer: true,
                // 用于匹配到main部分的路径，并用于后续的部分更改路径实现路由分离
                mainPathReg: pathToRegexp(`(${route.path})(/*)`)
            },
            components: {
                default: route.component,
                leftDrawer: dRoute.component
            }
        }
        routers.push(newRoute);
    });
}
for (let i = 0; i < len; ++i) {
    const route = routers[i];
    if (!route.meta) route.meta = {};
    route.meta.isMain = true;
}
drawerRouters.forEach((dRoute) => {
    if (!dRoute.meta) dRoute.meta = {};
    dRoute.meta.isDrawer = true;
});
console.log(routers);
const {router, noCacheList} = processRouters({
    routers
});

let curBlogPath = '/blog/index';
let curDrawerPath = '';
router.beforeEach((to, from, next) => {
    if (to.meta && to.meta.checkSign && !global.methods.checkSign()) {
        return;
    }
    if (to.path.indexOf('blog') === -1) {
        to.path = curBlogPath + to.path;
        next(to);
        global.states.blog.showDrawer = true;
        return;
    } else {
        if (to.meta && to.meta.isMain) {
            curBlogPath = to.path;
            if (curDrawerPath) {
                to.path = curBlogPath + curDrawerPath;
                next(to);
                return;
            }
        } else if (to.meta && to.meta.mainPathReg) {
            const execRes = to.meta.mainPathReg.exec(to.path);
            if (execRes.length > 1) curBlogPath = execRes[1];
            if (execRes.length > 2) curDrawerPath = execRes[2];
        }
        next();
    }
});

export {
    router as default,
    noCacheList
};