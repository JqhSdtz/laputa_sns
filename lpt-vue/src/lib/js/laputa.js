import axios from 'axios';

function initLaputa(option) {
    const lpt = {};
    lpt.isLocalHost = false;
    lpt.localhostUrl = 'http://localhost:8080/lpt';
    lpt.remoteUrl = 'https://lpt.ytumore.cn';
    lpt.imgBaseUrl = 'https://img.ytumore.cn/';

    lpt.loadOption = function (_option) {
        Object.assign(lpt, _option);
        lpt.baseUrl = lpt.isLocalHost ? lpt.localhostUrl : lpt.remoteUrl;
        lpt.baseStaticUrl = lpt.isLocalHost === 1 ? lpt.baseUrl : lpt.baseUrl + '/static';
    }
    lpt.loadOption(option);

    initCommonUtil();
    initUrlMethods();
    initAjaxMethods();
    initQueriorFactory();

    initAdminOpsService();
    initOperatorService();
    initUserService();
    initCategoryService();
    initPostService();
    initCommentService();
    initContentService();
    initFollowService();
    initForwardService();
    initLikeService();
    initNoticeService();
    initPermissionService();
    initNewsService();
    initSearchService();

    function initCommonUtil() {
        lpt.appendMethod = function (target, methodName, fun) {
            const ref = this;
            const oriFun = target[methodName];
            target[methodName] = function () {
                if (typeof oriFun == 'function')
                    oriFun.apply(ref, arguments);
                fun.apply(ref, arguments);
            }
        }
        lpt.isObject = function (obj) {
            return Object.prototype.toString.call(obj) === '[Object Object]';
        }
    }

    function wrap(fun) {
        const ref = this;
        return function () {
            for (let i = 0; i < arguments.length; ++i) {
                const arg = arguments[i];
                if (lpt.isObject(arg) && arg.url) {
                    // 该参数是对象，并且包含url属性，则为请求对象
                    if (!arg.data) {
                        // 防止data属性未定义引发错误
                        arg.data = {};
                    }
                    if (!arg.param) {
                        arg.param = {};
                    }
                }
            }
            return fun.apply(ref, arguments);
        };
    }

    function initUrlMethods() {
        lpt.getUserAvatarUrl = function (user) {
            if (typeof user.raw_avatar === 'undefined')
                return lpt.baseStaticUrl + '/img/def_ava.jpg';
            return lpt.imgBaseUrl + user.raw_avatar + '!ava.small/clip/50x50a0a0/gravity/center';
        }
        lpt.getPostThumbUrl = function (rawUrl) {
            return lpt.imgBaseUrl + rawUrl + '!/both/50x50';
        }
        lpt.getFullImgUrl = function (rawUrl) {
            return lpt.imgBaseUrl + rawUrl;
        }
    }

    function initAjaxMethods() {
        // 保存已发送的数据和URL及Method，用于判重
        lpt.sentData = new Set();
        lpt.ajax = function (param) {
            if (!param.url) {
                return {
                    success: 0,
                    message: 'URL不能为空！'
                };
            }
            const jsonDataStr = JSON.stringify(param.data);
            const method = param.method ? param.method.toUpperCase() : 'GET';
            // GET请求默认允许重复发送
            let allowRepeat = 'GET' === method.toUpperCase();
            if (typeof param.allowRepeat !== 'undefined')
                allowRepeat = param.allowRepeat;
            const dataStr = JSON.stringify({
                method: method,
                url: param.url,
                data: jsonDataStr
            });
            const sentData = param.querior ? param.querior.sentData : lpt.sentData;
            if (!allowRepeat && sentData.has(dataStr))
                return {
                    success: 0,
                    message: '当前请求不允许重复发送！'
                };
            sentData.add(dataStr);
            // 没有回调函数才使用promise
            const usePromise = !(param.success || param.fail || param.complete);
            const promise = axios({
                method: method,
                url: param.url,
                headers: {
                    'Content-Type': 'application/json'
                },
                responseType: 'json',
                param: method === 'GET' ? param.data : undefined,
                data: method === 'GET' ? undefined : jsonDataStr
            }).then(response => {
                const result = response.data;
                if (result.state === 1) {
                    if (typeof param.success === 'function')
                        param.success(result);
                    if (result.operator)
                        lpt.operatorServ.setCurrent(result.operator);
                } else {
                    if (typeof param.fail === 'function')
                        param.fail(result);
                    if (usePromise)
                        throw new Error(result.message);
                }
                if (typeof param.finish === 'function')
                    param.finish(result);
                if (typeof param.complete === 'function')
                    param.complete();
            }).catch(error => {
                if (typeof param.error === 'function')
                    param.error(error);
                if (typeof param.complete === 'function')
                    param.complete();
                throw error;
            });
            promise.success = 1;
            return promise;
        }
        lpt.get = function (param) {
            param.method = 'GET';
            return lpt.ajax(param);
        }
        lpt.post = function (param) {
            param.method = 'POST';
            return lpt.ajax(param);
        }
        lpt.patch = function (param) {
            param.method = 'PATCH';
            return lpt.ajax(param);
        }
        lpt.delete = function (param) {
            param.method = 'DELETE';
            return lpt.ajax(param);
        }
    }

    function initQueriorFactory() {
        lpt.defaultQueryParam = {
            query_num: 10,
            start_id: 0,
            from: 0
        };
        lpt.createQuerior = function (option) {
            const executor = {
                curStartId: 0,
                curFrom: 0,
                isSendingAjax: false,
                hasReachedBottom: false,
                totalLength: 0,
                curQueryToken: null,
                sentData: new Set()
            };
            Object.assign(executor, option);
            executor.onBusyChange = function (fun) {
                executor.onBusyChangeCallBack = fun;
            }
            executor.offBusyChange = function () {
                executor.onBusyChangeCallBack = undefined;
            }
            executor.query = function (param) {
                if (executor.isSendingAjax)
                    return executor;
                executor.isSendingAjax = true;
                if (executor.onBusyChangeCallBack)
                    executor.onBusyChangeCallBack(executor.isSendingAjax);
                param.data = param.data || {};
                param.data.query_param = Object.assign({}, lpt.defaultQueryParam, param.data.query_param);
                const queryParam = param.data.query_param;
                queryParam.start_id = executor.curStartId;
                queryParam.from = executor.curFrom;
                queryParam.query_token = executor.curQueryToken;
                lpt.appendMethod(param, 'success', function (result) {
                    if (result.object.length === 0) {
                        executor.hasReachedBottom = true;
                    } else {
                        executor.curStartId = result.object[result.object.length - 1].id;
                        executor.curFrom += result.object.length;
                        executor.totalLength += result.object.length;
                    }
                    if (result.attached_token)
                        executor.curQueryToken = result.attached_token;
                });
                lpt.appendMethod(param, 'complete', function () {
                    executor.isSendingAjax = false;
                    if (executor.onBusyChangeCallBack)
                        executor.onBusyChangeCallBack(executor.isSendingAjax);
                });
                if (param.method)
                    lpt.ajax(param);
                else
                    lpt.post(param);
                return executor;
            }
            executor.reset = function (param) {
                param = param || {};
                executor.curStartId = param.curStartId || 0;
                executor.curFrom = param.curFrom || 0;
                executor.isSendingAjax = param.isSendingAjax || false;
                executor.hasReachedBottom = param.hasReachedBottom || false;
                executor.totalLength = param.totalLength || 0;
                executor.curQueryToken = param.curQueryToken || null;
                executor.sentData.clear();
            }
            return executor;
        }
    }

    function initAdminOpsService() {
        const serv = {
            querior: lpt.createQuerior(),
            queryAdminOps: wrap(function (param) {
                param.url = lpt.baseUrl + '/admin_ops/list';
                return serv.querior.query(param);
            })
        };
        lpt.adminOpsServ = serv;
    }

    function initOperatorService() {
        const serv = {
            login: wrap(function (param) {
                param.url = lpt.baseUrl + '/operator/login';
                return lpt.post(param);
            }),
            logout: wrap(function (param) {
                param.url = lpt.baseUrl + '/operator/logout';
                return lpt.post(param);
            }),
            register: wrap(function (param) {
                param.url = lpt.baseUrl + '/operator/register';
                return lpt.post(param);
            }),
            setCurrent: wrap(function (_operator) {
                lpt.operator = _operator;
                localStorage.setItem('lpt_operator', JSON.stringify(_operator));
            }),
            getCurrent: wrap(function () {
                if (typeof lpt.operator !== 'undefined')
                    return lpt.operator;
                const st = localStorage.getItem('lpt_operator');
                return st === null ? null : JSON.parse(st);
            }),
            isCurrentAdmin: wrap(function (operator) {
                if (typeof operator === 'undefined')
                    operator = serv.getCurrent();
                const tmp = operator.permission_map;
                return typeof tmp !== 'undefined' && Object.keys(tmp).length !== 0;
            })
        };
        lpt.operatorServ = serv;
    }

    function initUserService() {
        const serv = {
            get: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/' + param.data.user_id;
                return lpt.get(param);
            }),
            checkName: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/check_name/' + param.data.userName;
                return lpt.get(param);
            }),
            setInfo: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/info';
                const operator = lpt.operatorServ.getCurrent();
                param.data.id = operator.user_id;
                return lpt.patch(param);
            }),
            setTopPost: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/top_post/create';
                return lpt.patch(param);
            }),
            cancelTopPost: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/top_post/cancel';
                return lpt.patch(param);
            }),
            getRecentVisit: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/recent_visit_categories';
                return lpt.get(param);
            }),
            pinRecentVisit: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/pin_category/' + param.data.categoryId;
                return lpt.post(param);
            }),
            unpinRecentVisit: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/unpin_category/' + param.data.categoryId;
                return lpt.post(param);
            })
        };
        lpt.userServ = serv;
    }

    function initPostService() {
        let currentCategory = {
            id: '0'
        };
        const serv = {
            setCurrentCategory: wrap(function (category) {
                currentCategory = category;
            }),
            getCurrentCategory: wrap(function () {
                return currentCategory;
            }),
            queryForCategory: wrap(function (param) {
                const queryType = param.data.queryType || 'popular';
                param.url = lpt.baseUrl + '/post/' + queryType;
                param.data.category_id = param.data.category_id || currentCategory.id;
                return param.querior.query(param);
            }),
            queryForCreator: wrap(function (param) {
                param.url = lpt.baseUrl + '/post/creator';
                return param.querior.query(param);
            }),
            getFullText: wrap(function (param) {
                param.url = lpt.baseUrl + '/post/full_text/' + param.data.fullTextId;
                return lpt.get(param);
            }),
            create: wrap(function (param) {
                const type = param.data.type || 'public';
                param.url = lpt.baseUrl + '/post/' + type;
                return lpt.post(param);
            }),
            setTopComment: wrap(function (param) {
                param.url = lpt.baseUrl + '/post/top_comment/create';
                return lpt.patch(param);
            }),
            cancelTopComment: wrap(function (param) {
                param.url = lpt.baseUrl + '/post/top_comment/cancel';
                return lpt.patch(param);
            })
        };
        lpt.postServ = serv;
    }

    function initCommentService() {
        const serv = {
            getQuerior: function (param) {
                const key = `${param.param.type}/${param.data.parentId}`;
                let querior = param.queriorMap.get(key);
                if (!querior) {
                    querior = lpt.createQuerior();
                    param.queriorMap.set(key, querior);
                }
                return querior;
            },
            query: wrap(function (param) {
                param.url = `${lpt.baseUrl}/comment/${param.param.type}/${param.param.rankType}/`;
                return serv.getQuerior(param).query(param);
            }),
            create: wrap(function (param) {
                param.url = lpt.baseUrl + '/comment/' + param.param.type;
                return lpt.post(param);
            })
        };
        lpt.commentServ = serv;
    }

    function initContentService() {
        const serv = {
            delete: wrap(function (param) {
                if (param.data.type == 'POST') {
                    param.url = lpt.baseUrl + '/post';
                } else if (param.data.type == 'CML1') {
                    param.url = lpt.baseUrl + '/comment/l1';
                } else if (param.data.type == 'CML2') {
                    param.url = lpt.baseUrl + '/comment/l2';
                } else
                    return;
                return lpt.delete(param);
            })
        };
        lpt.contentServ = serv;
    }

    function initCategoryService() {
        const serv = {
            setTopPost: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/top_post/' + (param.data.isCancel ? 'cancel' : 'create');
                return lpt.patch(param);
            }),
            setCacheNum: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/cache_num';
                return lpt.patch(param);
            }),
            setDispSeq: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/disp_seq';
                return lpt.patch(param);
            }),
            setDefaultSub: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/def_sub';
                return lpt.patch(param);
            }),
            setParent: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/parent';
                return lpt.patch(param);
            }),
            get: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/' + param.data.id;
                return lpt.get(param);
            }),
            create: wrap(function (param) {
                param.url = lpt.baseUrl + '/category';
                return lpt.post(param);
            }),
            delete: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/';
                return lpt.delete(param);
            }),
            getRoots: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/roots';
                return lpt.get(param);
            }),
            getDirectSub: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/direct_sub/' + param.data.categoryId;
                return lpt.get(param);
            }),
            getPathStr: function (list) {
                let pathStr = '';
                for (let i = list.length - 1; i >= 0; --i)
                    pathStr += list[i].name + '\\';
                return pathStr;
            }
        };
        lpt.categoryServ = serv;
    }

    function initFollowService() {
        const serv = {
            querior: lpt.createQuerior(),
            queryFollower: wrap(function (param) {
                param.url = lpt.baseUrl + '/follow/follower';
                return serv.querior.query(param);
            }),
            getFollowing: wrap(function (param) {
                param.url = lpt.baseUrl + '/follow/following/' + param.data.userId;
                return lpt.get(param);
            }),
            follow: wrap(function (param) {
                param.url = lpt.baseUrl + '/follow';
                return lpt.post(param);
            }),
            unFollow: wrap(function (param) {
                param.url = lpt.baseUrl + '/follow';
                return lpt.delete(param);
            })
        };
        lpt.followServ = serv;
    }

    function initForwardService() {
        const queriorMap = new Map();
        const serv = {
            getQuerior: function (supId) {
                const key = supId;
                let querior = queriorMap.get(key);
                if (!querior)
                    querior = lpt.createQuerior();
                return querior;
            },
            clearQueriors: function () {
                queriorMap.clear();
            },
            query: wrap(function (param) {
                param.url = lpt.baseUrl + '/forward/list';
                return serv.getQuerior(param.data.sup_id).query(param);
            }),
            create: wrap(function (param) {
                param.url = lpt.baseUrl + '/forward';
                return lpt.post(param);
            })
        };
        lpt.forwardServ = serv;
    }

    function initLikeService() {
        const serv = {
            querior: lpt.createQuerior(),
            query: wrap(function (param) {
                param.url = lpt.baseUrl + '/like/list/' + param.data.type;
                return serv.querior.query(param);
            }),
            like: wrap(function (param) {
                param.url = lpt.baseUrl + '/like/' + param.data.type;
                return lpt.post(param);
            }),
            unlike: wrap(function (param) {
                param.url = lpt.baseUrl + '/like/' + param.data.type;
                return lpt.delete(param);
            })
        };
        lpt.likeServ = serv;
    }

    function initNoticeService() {
        const serv = {
            querior: lpt.createQuerior(),
            query: wrap(function (param) {
                param.url = lpt.baseUrl + '/notice';
                return serv.querior.query(param);
            }),
            markAsRead: wrap(function (param) {
                param.url = lpt.baseUrl + '/notice/read';
                return lpt.post(param);
            })
        };
        lpt.noticeServ = serv;
    }

    function initPermissionService() {
        const serv = {
            getForCategory: wrap(function (param) {
                param.url = lpt.baseUrl + '/permission/category/' + param.data.category_id;
                return lpt.get(param);
            }),
            getForUser: wrap(function (param) {
                param.url = lpt.baseUrl + '/permission/user/' + param.data.user_id;
                return lpt.get(param);
            }),
            setAdmin: wrap(function (param) {
                param.url = lpt.baseUrl + '/permission';
                return lpt.post(param);
            }),
            delAdmin: wrap(function (param) {
                param.url = lpt.baseUrl + '/permission';
                return lpt.delete(param);
            }),
            updateAdmin: wrap(function (param) {
                param.url = lpt.baseUrl + '/permission';
                return lpt.patch(param);
            })
        };
        lpt.permissionServ = serv;
    }

    function initNewsService() {
        const serv = {
            querior: lpt.createQuerior(),
            query: wrap(function (param) {
                param.url = lpt.baseUrl + '/news';
                return serv.querior.query(param);
            })
        };
        lpt.newsServ = serv;
    }

    function initSearchService() {
        const serv = {
            search: wrap(function (param) {
                const data = param.data;
                param.url = `${lpt.baseUrl}/search/${data.searchType}/${data.words}/${data.mode}`;
                return lpt.get(param);
            })
        };
        lpt.searchServ = serv;
    }

    return lpt;
}

export default initLaputa();