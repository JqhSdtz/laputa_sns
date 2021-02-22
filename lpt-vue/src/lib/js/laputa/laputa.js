import axios from 'axios';

axios.defaults.withCredentials = true;

function throttle(fn, delay) {
    //https://github.com/ElemeFE/vue-infinite-scroll/blob/master/src/directive.js
    let now, lastExec, timer, context, args; //eslint-disable-line
    const execute = function () {
        fn.apply(context, args);
        lastExec = now;
    };
    return function () {
        context = this;
        args = arguments;
        now = Date.now();
        if (timer) {
            clearTimeout(timer);
            timer = null;
        }
        if (lastExec) {
            const diff = delay - (now - lastExec);
            if (diff < 0) {
                execute();
            } else {
                timer = setTimeout(() => {
                    execute();
                }, diff);
            }
        } else {
            execute();
        }
    };
}

function _initLaputa(option) {
    const lpt = {};
    lpt.isLocalHost = false;
    lpt.localhostUrl = 'http://localhost:8080/lpt';
    lpt.remoteUrl = 'https://lpt.jqh.zone';
    lpt.imgBaseUrl = 'https://img.jqh.zone/';

    lpt.loadOption = function (_option) {
        Object.assign(lpt, _option);
        lpt.baseUrl = lpt.isLocalHost ? lpt.localhostUrl : lpt.remoteUrl;
        lpt.baseStaticUrl = lpt.isLocalHost === 1 ? lpt.baseUrl : lpt.baseUrl + '/static';
    }
    lpt.loadOption(option);

    initCommonUtil();
    initEventBus();
    initUrlMethods();
    initAjaxMethods();
    initConsumerFactory();
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
        const util = {
            appendMethod(target, methodName, fun) {
                const ref = this;
                const oriFun = target[methodName];
                target[methodName] = function () {
                    if (typeof oriFun == 'function')
                        oriFun.apply(ref, arguments);
                    fun.apply(ref, arguments);
                }
            }
        }
        lpt.util = util;
    }

    function initEventBus() {
        const eventMap = new Map();
        const event = {};
        event.on = function (name, fun) {
            let funArr = eventMap.get(name);
            if (!funArr) {
                funArr = new Array();
                eventMap.set(name, funArr);
            }
            funArr.push(fun);
        }
        event.emit = function (name, obj) {
            const funArr = eventMap.get(name);
            if (!funArr)
                return;
            funArr.forEach(fun => {
                if (typeof fun === 'function')
                    fun(obj);
            });
        }
        event.off = function (name, fun) {
            const funArr = eventMap.get(name);
            funArr.forEach((_fun, idx) => {
                if (_fun === fun)
                    funArr.splice(idx, 1);
            });
        }
        lpt.event = event;
    }

    function initConsumerFactory() {
        lpt.createConsumer = function () {
            const consumer = {};
            const busyChangeListeners = [];
            let isBusy = false;
            let lastSentData;
            consumer.isBusy = function () {
                return isBusy;
            }
            consumer.changeBusy = function (_isBusy) {
                isBusy = _isBusy;
                busyChangeListeners.forEach(listener => {
                    if (typeof listener === 'function') {
                        listener(_isBusy);
                    }
                });
            }
            consumer.isDataRepeat = function (dataStr) {
                return lastSentData && lastSentData === dataStr;
            }
            consumer.setLastData = function (dataStr) {
                lastSentData = dataStr;
            }
            consumer.onBusyChange = function (listener) {
                busyChangeListeners.push(listener);
            }
            consumer.offBusyChange = function (listener) {
                if (listener) {
                    busyChangeListeners.forEach((item, idx) => {
                        if (item === listener)
                            busyChangeListeners.splice(idx, 1);
                    });
                }
            }
            return consumer;
        }
    }

    function wrap(fun) {
        const ref = this;
        const defaultParam = {
            allowRepeat: true
        }
        function processParam(param) {
            for (let key in defaultParam) {
                if (typeof param[key] === 'undefined')
                    param[key] = defaultParam[key];
            }
            if (!param.data) {
                // 防止data属性未定义引发错误
                param.data = {};
            }
            if (!param.param) {
                param.param = {};
            }
            if (typeof param.throwError === 'undefined') {
                // 没有回调函数才抛出异常以供catch捕获
                param.throwError = !(param.success || param.fail || param.complete);
            }
            param.appendMethod = function (name, fun) {
                lpt.util.appendMethod(param, name, fun);
            }
            param.appendSuccess = function (fun) {
                param.appendMethod('success', fun);
            }
            param.wrapped = true;
            return param;
        }

        return function () {
            const arg = arguments[0];
            if (arg && typeof arg === 'object') {
                // 该参数是请求对象
                processParam(arg);
            }
            const result = fun.apply(ref, arguments);
            if (result && result.from === 'local' && result.state === 0) {
                // 本地的失败信息在控制台输出
                console.log(result.message);
            }
            return result;
        };
    }

    function initUrlMethods() {
        lpt.getUserAvatarUrl = function (user) {
            if (!user.raw_avatar)
                return lpt.baseStaticUrl + '/img/def_ava.svg';
            return lpt.imgBaseUrl + user.raw_avatar + '!ava.small/clip/50x50a0a0/gravity/center';
        }
        lpt.getCategoryCoverUrl = function (category) {
            if (!category.cover_img)
                return lpt.baseStaticUrl + '/img/def_cat.svg';
            return lpt.imgBaseUrl + category.cover_img + '!cat.standard';
        }
        lpt.getCategoryIconUrl = function (category) {
            if (!category.icon_img)
                return lpt.baseStaticUrl + '/img/def_cat.svg';
            return lpt.imgBaseUrl + category.icon_img +  '!cat.small/clip/50x50a0a0/gravity/center';
        }
        lpt.getPostThumbUrl = function (rawUrl) {
            return lpt.imgBaseUrl + rawUrl + '!/both/50x50';
        }
        lpt.getFullImgUrl = function (rawUrl) {
            return lpt.imgBaseUrl + rawUrl;
        }
    }

    function initAjaxMethods() {
        const lptUserTokenIdentifier = 'l7p$t-u8s*e6r-t5o@k4e(3$n1~';
        let curUserToken = localStorage.getItem(lptUserTokenIdentifier);
        lpt.getCurUserToken = function () {
            return curUserToken;
        }

        function changeBusy(param, isBusy) {
            lpt.event.emit('pushGlobalBusy', {
                isBusy: isBusy,
                ignoreGlobalBusyChange: param.ignoreGlobalBusyChange
            });
            if (!param.ignoreBusyChange) {
                // 发起consumer内部繁忙状态改变
                param.consumer.changeBusy(isBusy);
            }
        }

        lpt.ajax = function (param) {
            if (!param.consumer) {
                // console.warn('no consumer defined for ajax request! use new consumer.');
                param.consumer = lpt.createConsumer();
            }
            const consumer = param.consumer;
            if (!param.url) {
                return {
                    success: 0,
                    from: 'local',
                    message: 'URL不能为空！'
                };
            }
            const jsonDataStr = JSON.stringify(param.data);
            const method = param.method ? param.method.toUpperCase() : 'GET';
            // // GET请求默认允许重复发送
            // let allowRepeat = 'GET' === method.toUpperCase();
            // if (typeof param.allowRepeat !== 'undefined')
            //     allowRepeat = param.allowRepeat;
            // 改成全部按默认值
            const allowRepeat = param.allowRepeat;
            const dataStr = JSON.stringify({
                method: method,
                url: param.url,
                data: jsonDataStr
            });
            if (!allowRepeat && consumer.isDataRepeat(dataStr))
                return {
                    success: 0,
                    from: 'local',
                    message: '当前请求不允许重复发送！'
                };
            if (param.consumer.isBusy()) {
                return {
                    success: 0,
                    from: 'local',
                    message: '正在执行其他请求!'
                };
            }
            changeBusy(param, true);
            const headers = {
                'Content-Type': 'application/json'
            };
            if (curUserToken) {
                headers['X-LPT-USER-TOKEN'] = curUserToken;
            }
            function onComplete(param, result) {
                if (typeof param.complete === 'function')
                    param.complete(result);
                changeBusy(param, false);
                consumer.setLastData(dataStr);
            }
            const promise = axios({
                method: method,
                url: param.url,
                headers: headers,
                responseType: 'json',
                param: method === 'GET' ? param.data : undefined,
                data: method === 'GET' ? undefined : jsonDataStr
            }).then(response => {
                const tmpUserToken = response.headers['x-lpt-user-token'];
                if (typeof tmpUserToken !== 'undefined') {
                    curUserToken = tmpUserToken;
                    localStorage.setItem(lptUserTokenIdentifier, curUserToken);
                }
                const result = response.data;
                if (result.state === 1) {
                    if (typeof param.success === 'function')
                        param.success(result);
                    if (result.operator)
                        lpt.operatorServ.setCurrent(result.operator);
                } else {
                    if (typeof param.fail === 'function')
                        param.fail(result);
                    if (param.throwError) {
                        onComplete(param, result);
                        // 如果有throwObject选项，则抛出整个result对象，否则抛出错误信息
                        throw new Error(param.throwObject ? result : result.message);
                    }
                }
                if (typeof param.finish === 'function')
                    param.finish(result);
                onComplete(param, result);
                return Promise.resolve(result);
            }).catch(error => {
                if (typeof param.error === 'function')
                    param.error(error);
                onComplete(param, error);
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
                hasReachedBottom: false,
                totalLength: 0,
                curQueryToken: null
            };
            // 每个查询器都内含一个消费者
            const consumer = lpt.createConsumer();
            Object.assign(executor, option);
            let onBusyChangeCallBack;
            executor.onBusyChange = function (fun) {
                onBusyChangeCallBack = fun;
                consumer.onBusyChange(fun);
            }
            executor.offBusyChange = function () {
                consumer.offBusyChange(onBusyChangeCallBack);
            }
            executor.isBusy = function () {
                return consumer.isBusy();
            }
            executor.query = throttle(function (param) {
                if (consumer.isBusy())
                    return executor;
                param.data = param.data || {};
                param.data.query_param = Object.assign({}, lpt.defaultQueryParam, param.data.query_param);
                const queryParam = param.data.query_param;
                queryParam.start_id = executor.curStartId;
                queryParam.from = executor.curFrom;
                queryParam.query_token = executor.curQueryToken;
                // 携带当前查询器的内含消费者信息
                param.consumer = consumer;
                // 查询器允许POST重复数据
                param.allowRepeat = true;
                param.appendSuccess(result => {
                    const queryStep = param.data.query_param.query_num;
                    const resultLen = result.object.length;
                    if (resultLen === 0) {
                        executor.hasReachedBottom = true;
                    } else if (queryStep && resultLen < queryStep) {
                        executor.hasReachedBottom = true;
                    } else {
                        executor.curStartId = result.object[result.object.length - 1].id;
                        executor.curFrom += result.object.length;
                        executor.totalLength += result.object.length;
                    }
                    if (result.attached_token)
                        executor.curQueryToken = result.attached_token;
                });
                const res = param.method ? lpt.ajax(param) : lpt.post(param);
                return res;
            }, 600);
            executor.reset = function (param) {
                param = param || {};
                executor.curStartId = param.curStartId || 0;
                executor.curFrom = param.curFrom || 0;
                executor.hasReachedBottom = param.hasReachedBottom || false;
                executor.totalLength = param.totalLength || 0;
                executor.curQueryToken = param.curQueryToken || null;
            }
            lpt.event.emit('createQuerior', executor);
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
        const unSignedOperator = {
            user: {
                id: -1
            },
            unread_notice_cnt: 0,
            unread_news_cnt: 0,
            permission_map: {}
        };
        let curOperator;
        const serv = {
            signIn: wrap(function (param) {
                param.url = lpt.baseUrl + '/operator/login';
                // 登录允许POST重复数据
                param.allowRepeat = true;
                param.appendSuccess(result => {
                    lpt.operatorServ.setCurrent(result.object);
                });
                return lpt.post(param);
            }),
            signOut: wrap(function (param) {
                // 请求数据自动处理过程中已经修改了currentOperator
                param.url = lpt.baseUrl + '/operator/logout';
                return lpt.post(param);
            }),
            signUp: wrap(function (param) {
                param.url = lpt.baseUrl + '/operator/register';
                param.allowRepeat = false;
                param.appendSuccess(result => {
                    lpt.operatorServ.setCurrent(result.object);
                });
                return lpt.post(param);
            }),
            hasSigned: wrap(function () {
                return serv.getCurrent().user.id !== -1;
            }),
            setCurrent: function (_operator) {
                if (_operator.user_id === -1) {
                    _operator.unread_notice_cnt = 0;
                    _operator.unread_news_cnt = 0;
                } else {
                    const permissionMap = _operator.permission_map;
                    if (permissionMap && Object.keys(permissionMap).length > 0) {
                        _operator.isAdmin = true;
                    }
                }
                curOperator = _operator;
                localStorage.setItem('lpt_operator', JSON.stringify(_operator));
                lpt.event.emit('onCurOperatorChange', _operator);
            },
            getCurrent: wrap(function () {
                if (typeof curOperator !== 'undefined')
                    return curOperator;
                const st = localStorage.getItem('lpt_operator');
                return (st === null || st === 'undefined') ? unSignedOperator : JSON.parse(st);
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
            getDefaultUser: function(id) {
                return {
                    isDefault: true,
                    id: id,
                    nick_name: '',
                    raw_avatar: '',
                    followers_cnt: 0,
                    following_cnt: 0,
                    post_cnt: 0,
                    top_post_id: -1
                }
            },
            get: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/' + param.param.userId;
                return lpt.get(param);
            }),
            getInfo: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/info/' + param.param.userId;
                return lpt.get(param);
            }),
            checkName: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/check_name/' + param.param.userName;
                return lpt.get(param);
            }),
            setInfo: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/info';
                // 允许改来改去
                param.allowRepeat = true;
                return lpt.patch(param);
            }),
            updateUserName: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/name';
                // 允许改来改去
                param.allowRepeat = true;
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
                param.url = lpt.baseUrl + '/user/pin_category/' + param.param.categoryId;
                return lpt.post(param);
            }),
            unpinRecentVisit: wrap(function (param) {
                param.url = lpt.baseUrl + '/user/unpin_category/' + param.param.categoryId;
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
            getDefaultPost: function(id) {
                return {
                    isDefault: true,
                    id: id,
                    creator: {},
                    rights: {},
                    liked_by_viewer: false,
                    forward_cnt: 0,
                    comment_cnt: 0,
                    like_cnt: 0,
                    category_path: []
                };
            },
            setCurrentCategory: wrap(function (category) {
                currentCategory = category;
            }),
            getCurrentCategory: wrap(function () {
                return currentCategory;
            }),
            queryForCategory: wrap(function (param) {
                const queryType = param.param.queryType || 'popular';
                param.url = lpt.baseUrl + '/post/' + queryType;
                param.data.category_id = param.data.category_id || currentCategory.id;
                return param.querior.query(param);
            }),
            queryForCreator: wrap(function (param) {
                param.url = lpt.baseUrl + '/post/creator';
                return param.querior.query(param);
            }),
            getFullText: wrap(function (param) {
                param.url = lpt.baseUrl + '/post/full_text/' + param.param.fullTextId;
                return lpt.get(param);
            }),
            create: wrap(function (param) {
                const type = param.param.type || 'public';
                param.url = lpt.baseUrl + '/post/' + type;
                return lpt.post(param);
            }),
            get: wrap(function (param) {
                param.url = lpt.baseUrl + '/post/' + param.param.postId;
                return lpt.get(param);
            }),
            setTopComment: wrap(function (param) {
                param.url = lpt.baseUrl + '/post/top_comment/' + (param.param.isCancel ? 'cancel' : 'create');
                return lpt.patch(param);
            })
        };
        lpt.postServ = serv;
    }

    function initCommentService() {
        const defaultComment = {
            isDefault: true,
            creator: {},
            rights: {},
            liked_by_viewer: false,
            like_cnt: 0
        }
        const serv = {
            getDefaultCommentL1: function (id){
                return {
                    id: id,
                    ...defaultComment,
                    comment_cnt: 0
                };
            },
            getDefaultCommentL2: function(id) {
                return {
                    id: id,
                    ...defaultComment
                };
            },
            level1: 'l1',
            level2: 'l2',
            get: wrap(function (param) {
                param.url = `${lpt.baseUrl}/comment/${param.param.type}/${param.param.commentId}`;
                return lpt.get(param);
            }),
            query: wrap(function (param) {
                param.url = `${lpt.baseUrl}/comment/${param.param.type}/${param.param.queryType}`;
                return param.querior.query(param);
            }),
            create: wrap(function (param) {
                param.url = `${lpt.baseUrl}/comment/${param.param.type}`;
                return lpt.post(param);
            })
        };
        lpt.commentServ = serv;
    }

    function initContentService() {
        lpt.contentType = {
            post: 'post',
            commentL1: 'cml1',
            commentL2: 'cml2'
        }
        const serv = {
            delete: wrap(function (param) {
                if (param.param.type == 'post') {
                    param.url = lpt.baseUrl + '/post';
                } else if (param.param.type == 'cml1') {
                    param.url = lpt.baseUrl + '/comment/l1';
                } else if (param.param.type == 'cml2') {
                    param.url = lpt.baseUrl + '/comment/l2';
                } else
                    return;
                return lpt.delete(param);
            })
        };
        lpt.contentServ = serv;
    }

    function initCategoryService() {
        function processList(list) {
            // 对目录按照disp_seq排序
            if (!list)
                return;
            list.forEach(category => category.disp_seq = category.disp_seq || 0);
            list.sort((c1, c2) => {return c1 > c2});
        }
        const serv = {
            getDefaultCategory: function(id) {
                return {
                    isDefault: true,
                    id: id,
                    name: '',
                    top_post_id: -1,
                    sub_list: [],
                    cover_img: '',
                    icon_img: '',
                    rights: {},
                    disp_seq: 0
                }
            },
            setInfo: wrap(function (param) {
               param.url = lpt.baseUrl + '/category/info';
               return lpt.patch(param);
            }),
            setTopPost: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/top_post/' + (param.param.isCancel ? 'cancel' : 'create');
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
                param.url = lpt.baseUrl + '/category/' + param.param.id;
                return lpt.get(param).then(result => processList(result.object.sub_list));
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
                return lpt.get(param).then(result => processList(result.object));
            }),
            getDirectSub: wrap(function (param) {
                param.url = lpt.baseUrl + '/category/direct_sub/' + param.data.categoryId;
                return lpt.get(param).then(result => processList(result.object));
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
            queryFollower: wrap(function (param) {
                param.url = lpt.baseUrl + '/follow/follower';
                return param.querior.query(param);
            }),
            getFollowing: wrap(function (param) {
                param.url = lpt.baseUrl + '/follow/following/' + param.param.userId;
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
        const serv = {
            query: wrap(function (param) {
                param.url = lpt.baseUrl + '/forward/list';
                return param.querior.query(param);
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
            query: wrap(function (param) {
                param.url = lpt.baseUrl + '/like/list/' + param.param.type;
                return param.querior.query(param);
            }),
            like: wrap(function (param) {
                param.url = lpt.baseUrl + '/like/' + param.param.type;
                return lpt.post(param);
            }),
            unlike: wrap(function (param) {
                param.url = lpt.baseUrl + '/like/' + param.param.type;
                return lpt.delete(param);
            })
        };
        lpt.likeServ = serv;
    }

    function initNoticeService() {
        const serv = {
            getDefaultNotice(id) {
                return {
                    id: id
                }
            },
            query: wrap(function (param) {
                param.url = lpt.baseUrl + '/notice';
                return param.querior.query(param);
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
                param.url = lpt.baseUrl + '/permission/category/' + param.param.categoryId;
                return lpt.get(param);
            }),
            getForUser: wrap(function (param) {
                param.url = lpt.baseUrl + '/permission/user/' + param.param.userId;
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
            query: wrap(function (param) {
                param.url = lpt.baseUrl + '/news';
                return param.querior.query(param);
            })
        };
        lpt.newsServ = serv;
    }

    function initSearchService() {
        const serv = {
            search: wrap(function (param) {
                param.url = `${lpt.baseUrl}/search/${param.param.searchType}/${param.param.words}/${param.param.mode}`;
                return lpt.get(param);
            })
        };
        lpt.searchServ = serv;
    }
    return lpt;
}

export default _initLaputa();