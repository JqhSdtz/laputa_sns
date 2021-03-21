import {processRouters} from '@/lib/js/router/util';
import Home from '@/modules/blog/pages/home/Home';
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

const routers = [
    {
        path: '/',
        redirect: '/home'
    },
    {
        path: '/home',
        name: 'home',
        components: {
            default: Home
        }
    },
    {
        path: '/mine',
        name: 'mine',
        components: {
            default: Home,
            leftDrawer: Mine
        }
    },
    {
        path: '/mod_user_info',
        name: 'modUserInfo',
        components: {
            default: Home,
            leftDrawer: ModUserInfo
        }
    },
    {
        path: '/sign_in',
        name: 'signIn',
        components: {
            default: Home,
            leftDrawer: SignIn
        }
    },
    {
        path: '/sign_up',
        name: 'signUp',
        components: {
            default: Home,
            leftDrawer: SignUp
        }
    },
    {
        path: '/post_detail/:postId',
        name: 'postDetail',
        components: {
            default: Home,
            leftDrawer: PostDetail
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/publish',
        name: 'publish',
        components: {
            default: Home,
            leftDrawer: Publish
        },
        meta: {
            checkSign: true
        }
    },
    {
        path: '/user_home_page/:userId',
        name: 'userHomePage',
        components: {
            default: Home,
            leftDrawer: UserHomePage
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/followers_list/:userId',
        name: 'followersList',
        components: {
            default: Home,
            leftDrawer: FollowersList
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/following_list/:userId',
        name: 'followingList',
        components: {
            default: Home,
            leftDrawer: FollowingList
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/notice_list',
        name: 'noticeList',
        components: {
            default: Home,
            leftDrawer: NoticeList
        },
    },
    {
        path: '/comment_like_list/:type/:commentId',
        name: 'commentLikeList',
        components: {
            default: Home,
            leftDrawer: CommentLikeList
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/search_index',
        name: 'searchIndex',
        components: {
            default: Home,
            leftDrawer: SearchIndex
        },
        meta: {
            noCache: true
        }
    },
    {
        path: '/category_detail/:categoryId',
        name: 'categoryDetail',
        components: {
            default: Home,
            leftDrawer: CategoryDetail
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/category_setting/:categoryId',
        name: 'categorySetting',
        components: {
            default: Home,
            leftDrawer: CategorySetting
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/mod_category_info/:categoryId',
        name: 'modCategoryInfo',
        components: {
            default: Home,
            leftDrawer: ModCategoryInfo
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/permission_list/:userId',
        name: 'permissionList',
        components: {
            default: Home,
            leftDrawer: PermissionList
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/category_admin_list/:categoryId',
        name: 'categoryAdminList',
        components: {
            default: Home,
            leftDrawer: CategoryAdminList
        },
        props: true,
        meta: {
            noCache: true
        }
    },
    {
        path: '/druid_stat',
        name: 'druidStat',
        components: {
            default: Home,
            leftDrawer: DruidStat
        }
    }
];
const {router, noCacheList} = processRouters({
    routers,
    homePath: 'home',
    homeIndexPath: '/home'
});

export {
    router as default,
    noCacheList
};