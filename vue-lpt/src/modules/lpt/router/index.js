import Home from '@/modules/lpt/components/home/Home'
// 修改目录时Intellij IDEA会将index直接省去，然后vue编译时会报错，加上index
import Index from '@/modules/lpt/pages/inside/index/Index';
import News from '@/modules/lpt/pages/inside/news/News';
import Community from '@/modules/lpt/pages/inside/community/Community';
import Mine from '@/modules/lpt/pages/inside/mine/Mine';
import ModUserInfo from '@/modules/lpt/pages/outside/user/ModUserInfo';
import SignIn from '@/modules/lpt/pages/outside/sign/SignIn';
import SignUp from '@/modules/lpt/pages/outside/sign/SignUp';
import PostDetail from '@/components/post/post_detail/PostDetail';
import Publish from '@/modules/lpt/pages/outside/publish/Publish';
import UserHomePage from '@/modules/lpt/pages/outside/user/UserHomePage';
import FollowersList from '@/modules/lpt/pages/outside/user/user_list/FollowersList';
import FollowingList from '@/modules/lpt/pages/outside/user/user_list/FollowingList';
import NoticeList from '@/modules/lpt/pages/outside/notice/NoticeList';
import CommentLikeList from '@/components/post/post_detail/comment_detail/CommentLikeList';
import SearchIndex from '@/modules/lpt/pages/outside/search/SearchIndex';
import CategoryDetail from '@/modules/lpt/pages/outside/category/detail/CategoryDetail';
import CategorySetting from '@/modules/lpt/pages/outside/category/CategorySetting';
import ModCategoryInfo from '@/modules/lpt/pages/outside/category/ModCategoryInfo';
import PermissionList from '@/modules/lpt/pages/outside/user/PermissionList';
import CategoryAdminList from '@/modules/lpt/pages/outside/category/CategoryAdminList';
import DruidStat from '@/modules/lpt/pages/outside/DruidStat';
import {processRouters} from '@/lib/js/router/util';
import LptQrCode from "@/modules/lpt/pages/inside/mine/LptQrCode";
import Report from "@/modules/lpt/pages/outside/report/Report";
import global from "@/lib/js/global";

const routers = [
    {
        path: '/',
        redirect: '/home/index'
    },
    {
        path: '/lpt',
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
                component: News,
                meta: {
                    checkSign: true
                }
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
        props: true
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
        path: '/mod_category_info/:categoryId?',
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
        path: '/lpt_qr_code',
        name: 'lptQrCode',
        component: LptQrCode
    },
    {
        path: '/report',
        name: 'report',
        component: Report
    }
];

const {router, noCacheList, titleKeeper} = processRouters({
    routers
});

let curHomeInsidePath = '/home/index';
const homePath = '/home/index';

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

router.beforeEach(titleKeeper);

export {
    router as default,
    noCacheList
};