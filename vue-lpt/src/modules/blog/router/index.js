import {processRouters} from '@/lib/js/router/util';
import lpt from '@/lib/js/laputa/laputa';
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
import Categories from "@/modules/blog/pages/category/Categories";

function initRouter() {
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
        },
        {
            path: '/categories',
            name: 'categories',
            component: Categories
        }
    ];
    let curBlogPath = '/blog/index/' + lpt.categoryServ.rootCategoryId;
    let curDrawerPath = '/home/mine';
    const mainRouters = [
        {
            path: '/',
            redirect: curBlogPath + curDrawerPath
        },
        {
            path: '/blog/index/:blogCategoryId',
            component: Index,
            props: route => ({
                categoryId: route.params.blogCategoryId
            }),
            meta: {
                mainPathPropNum: 1
            }
        },
        {
            path: '/blog/post_detail/:blogPostId',
            name: 'blogPostDetail',
            component: PostDetail,
            props: route => ({
                postId: route.params.blogPostId
            }),
            meta: {
                noCache: true,
                mainPathPropNum: 1
            }
        },
    ];
    mainRouters.forEach((route) => {
        if (!route.meta) route.meta = {};
        route.meta.isMain = true;
    });
    drawerRouters.forEach((dRoute) => {
        if (!dRoute.meta) dRoute.meta = {};
        dRoute.meta.isDrawer = true;
    });
    const routers = mainRouters.concat(drawerRouters);

// 以下循环创建路由项，为每一个主页面和抽屉页面的组合都创建一个路由
// 以此实现主页面和抽屉页面分别路由
    mainRouters.forEach((route) => {
        if (route.path === '/')
            return;
        drawerRouters.forEach((dRoute) => {
            const newRoute = {
                path: route.path + dRoute.path,
                name: (route.name || '') + '_' + (dRoute.name || ''),
                props: {
                    default: route.props,
                    leftDrawer: dRoute.props
                },
                meta: {
                    ...route.meta,
                    ...dRoute.meta,
                    combined: true,
                    mainPath: route.path,
                    drawerPath: dRoute.path,
                    // 用于匹配到main部分的路径，并用于后续的部分更改路径实现路由分离
                    mainPathReg: pathToRegexp(`(${route.path})(\/*)`)
                },
                components: {
                    default: route.component,
                    leftDrawer: dRoute.component
                }
            }
            routers.push(newRoute);
        });
    });
    const res = processRouters({
        routers
    });
    router = res.router;
    noCacheList = res.noCacheList;

    let isRouterBack = false;
    const oriFun = router.push;
    router.push = function (to) {
        isRouterBack = false;
        return oriFun.apply(router, arguments);
    }

    let blogMainHistoryStack = [];
    router.beforeEach((to, from, next) => {
        if (to.meta.checkSign && !global.methods.checkSign()) {
            return;
        } else if (isRouterBack && !global.states.blog.showDrawer) {
            // 当前是主页面回退，要回到上一个主页面
            if (blogMainHistoryStack.length <= 1) {
                next();
            } else {
                blogMainHistoryStack.pop();
                const preMainPath = blogMainHistoryStack[blogMainHistoryStack.length - 1];
                const newTo = {
                    path: preMainPath + curDrawerPath,
                    query: to.query,
                    params: to.params
                }
                next(newTo);
            }
            isRouterBack = false;
        } else if (to.meta.combined) {
            const execRes = to.meta.mainPathReg.exec(to.path);
            if (execRes.length > 1) {
                curBlogPath = execRes[1];
                // 主页面历史堆栈为空，则是直接从组合路径进入，此时要向主页面历史堆栈推入当前主页面路径
                if (blogMainHistoryStack.length === 0) blogMainHistoryStack.push(curBlogPath);
            }
            const mainPathPropNum = to.meta.mainPathPropNum || 0;
            const drawerPathPos = 2 + mainPathPropNum;
            if (execRes.length > drawerPathPos) curDrawerPath = execRes[drawerPathPos];
            next();
            isRouterBack = true;
        } else if (to.meta.isMain) {
            curBlogPath = to.path;
            blogMainHistoryStack.push(curBlogPath);
            if (curDrawerPath) {
                next({
                    path: curBlogPath + curDrawerPath,
                    query: to.query,
                    params: to.params
                });
            } else {
                next();
            }
        } else if (to.meta.isDrawer) {
            next({
                path: curBlogPath + to.path,
                query: to.query,
                params: to.params
            });
            global.states.blog.showDrawer = true;
        }
    });
    return;
}

let router, noCacheList

export {
    router as default,
    noCacheList,
    initRouter
};