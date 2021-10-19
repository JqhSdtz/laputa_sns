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
    // lpt.remoteUrl = 'https://jqh.zone';
    // 配置vue的proxy时需要使用相对地址，部署后也使用相对地址即可
    lpt.remoteUrl = '';
    lpt.imgBaseUrl = 'https://img.jqh.zone/';

    lpt.loadOption = function (_option) {
        Object.assign(lpt, _option);
        lpt.baseUrl = lpt.isLocalHost ? lpt.localhostUrl : lpt.remoteUrl;
        lpt.baseApiUrl = lpt.baseUrl + '/api';
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
    initQQService();
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
    initCorrectService();

    function initCommonUtil() {
        const util = {
            appendMethod(target, methodName, fun, insertBefore) {
                const ref = this;
                const oriFun = target[methodName];
                target[methodName] = function () {
                    if (insertBefore) {
                        fun.apply(ref, arguments);
                        if (typeof oriFun == 'function')
                            oriFun.apply(ref, arguments);
                    } else {
                        if (typeof oriFun == 'function')
                            oriFun.apply(ref, arguments);
                        fun.apply(ref, arguments);
                    }
                }
            },
            getCookie(name) {
                name += '=';
                const cookies = document.cookie.split(';');
                let res = '';
                cookies.forEach((cookie) => {
                    const str = cookie.trim();
                    if (str.indexOf(name) === 0)
                        res = str.substring(name.length, str.length)
                });
                return res;
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
            let busyCount = 0;
            consumer.isBusy = function () {
                return isBusy;
            }
            consumer.pushBusy = function (_isBusy) {
                const oriBusyCount = busyCount;
                busyCount += _isBusy ? 1 : -1;
                if (Math.sign(oriBusyCount) !== Math.sign(busyCount)) {
                    isBusy = _isBusy;
                    busyChangeListeners.forEach(listener => {
                        if (typeof listener === 'function') {
                            listener(isBusy);
                        }
                    });
                }
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
            allowRepeat: true,
            ignoreBusyChange: false,
            objectOnly: false,
            filterEmpty: true,
            useResultFormat: true,
            responseType: 'json'
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
            if (param.responseType !== 'json') param.useResultFormat = false;
            param.appendMethod = function (name, fun, insertBefore) {
                lpt.util.appendMethod(param, name, fun, insertBefore);
            }
            param.appendSuccess = function (fun, insertBefore) {
                param.appendMethod('success', fun, insertBefore);
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
            return lpt.imgBaseUrl + category.icon_img + '!cat.small/clip/75x75a0a0/gravity/center';
        }
        lpt.getPostThumbUrl = function (rawUrl) {
            return lpt.imgBaseUrl + rawUrl + '!/both/50x50';
        }
        lpt.getFullImgUrl = function (rawUrl, width) {
            if (width) {
                return lpt.imgBaseUrl + rawUrl + '!/fw/' + width;
            } else {
                return lpt.imgBaseUrl + rawUrl;
            }
        }
    }

    function initAjaxMethods() {
        const lptUserTokenIdentifier = 'l7p$t-u8s*e6r-t5o@k4e(3$n1~';
        // 优先从cookie中获取，cookie可以解决跨子域名问题
        let curUserToken = lpt.util.getCookie('token');
        if (!curUserToken) {
            // cookie没有，则从localStorage获取
            curUserToken = localStorage.getItem(lptUserTokenIdentifier);
        }
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
                param.consumer.pushBusy(isBusy);
            }
        }

        let firstAjax;
        lpt.ajax = function (param) {
            // 要等到第一个ajax请求执行完后再执行后续请求
            // 防止需要更换token时，同时发出去多个ajax请求，导致token混乱
            if (firstAjax && !firstAjax.settled) {
                return firstAjax.then(() => lpt._ajax(param));
            } else {
                const ajaxPromise = lpt._ajax(param);
                if (!firstAjax) {
                    firstAjax = ajaxPromise;
                }
                return ajaxPromise;
            }
        }
        lpt._ajax = function (param) {
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
                promise.settled = true;
                if (typeof param.complete === 'function')
                    param.complete(result);
                changeBusy(param, false);
                consumer.setLastData(dataStr);
            }

            const promise = axios({
                method: method,
                url: param.url,
                headers: headers,
                responseType: param.responseType,
                param: method === 'GET' ? param.data : undefined,
                data: method === 'GET' ? undefined : jsonDataStr
            }).then(response => {
                const result = response.data;
                const tmpUserToken = response.headers['x-lpt-user-token'];
                if (typeof tmpUserToken !== 'undefined') {
                    curUserToken = tmpUserToken;
                    localStorage.setItem(lptUserTokenIdentifier, curUserToken);
                }
                if (result.operator)
                    lpt.operatorServ.setCurrent(result.operator);
                if (!param.useResultFormat || result.state === 1) {
                    if (param.filterEmpty && result.object instanceof Array) {
                        result.object = result.object.filter(obj => obj);
                    }
                    if (typeof param.success === 'function')
                        param.success(result);
                } else {
                    if (typeof param.fail === 'function')
                        param.fail(result);
                    if (param.throwError) {
                        onComplete(param, result);
                        // 如果有throwObject选项，则抛出整个result对象，否则抛出错误信息
                        return Promise.reject(param.throwObject ? result : result.message);
                    }
                }
                if (typeof param.finish === 'function')
                    param.finish(result);
                onComplete(param, result);
                return Promise.resolve(param.objectOnly ? result.object : result);
            }).catch(error => {
                if (typeof param.error === 'function')
                    param.error(error);
                onComplete(param, error);
                return Promise.reject(error);
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
                    const resultLen = result.object.length;
                    if (resultLen === 0) {
                        executor.hasReachedBottom = true;
                    } else {
                        executor.curStartId = result.object[result.object.length - 1].id;
                        executor.curFrom += result.object.length;
                        executor.totalLength += result.object.length;
                    }
                    if (result.attached_token) {
                        const tokenParts = result.attached_token.split('#');
                        if (tokenParts.length !== 0 && '1' === tokenParts[0]) {
                            executor.hasReachedBottom = true;
                        }
                        executor.curQueryToken = result.attached_token;
                    }
                }, true);
                const res = param.method ? lpt.ajax(param) : lpt.post(param);
                return res;
            }, 1000);
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
                param.url = lpt.baseApiUrl + '/admin_ops/list';
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
            permission_map: {},
            from_login: false,
            from_register: false
        };
        let curOperator;
        const serv = {
            signIn: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/operator/login';
                param.appendSuccess(result => {
                    lpt.operatorServ.setCurrent(result.object);
                });
                return lpt.post(param);
            }),
            signOut: wrap(function (param) {
                // 请求数据自动处理过程中已经修改了currentOperator
                param.url = lpt.baseApiUrl + '/operator/logout';
                return lpt.post(param);
            }),
            signUp: wrap(function (param) {
                const p = param.param;
                param.url = `${lpt.baseApiUrl}/operator/register/${p.extra64}/${p.rand}/${p.calRes}`;
                param.allowRepeat = false;
                param.appendSuccess(result => {
                    lpt.operatorServ.setCurrent(result.object);
                });
                return lpt.post(param);
            }),
            emptyAction: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/operator/empty';
                return lpt.get(param);
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
            getDefaultUser: function (id) {
                return {
                    isDefault: true,
                    id: id,
                    nick_name: '',
                    intro: '',
                    raw_avatar: '',
                    followers_cnt: 0,
                    following_cnt: 0,
                    post_cnt: 0,
                    top_post_id: -1
                }
            },
            get: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/' + param.param.userId;
                return lpt.get(param);
            }),
            getByName: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/name/' + param.param.userName;
                return lpt.get(param);
            }),
            getInfo: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/info/' + param.param.userId;
                return lpt.get(param);
            }),
            checkName: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/check_name/' + param.param.userName;
                return lpt.get(param);
            }),
            setInfo: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/info';
                return lpt.patch(param);
            }),
            updateUserName: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/name';
                return lpt.patch(param);
            }),
            updatePassword: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/password';
                return lpt.patch(param);
            }),
            setTopPost: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/top_post/create';
                return lpt.patch(param);
            }),
            cancelTopPost: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/top_post/cancel';
                return lpt.patch(param);
            }),
            setTalkBan: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/set_talk_ban';
                return lpt.patch(param);
            }),
            getRecentVisit: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/recent_visit_categories';
                param.appendSuccess(result => {
                    result.object = result.object.filter(obj => obj.category);
                }, true);
                return lpt.get(param);
            }),
            pinRecentVisit: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/pin_category/' + param.param.categoryId;
                return lpt.post(param);
            }),
            unpinRecentVisit: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/user/unpin_category/' + param.param.categoryId;
                return lpt.post(param);
            })
        };
        lpt.userServ = serv;
    }

    function initQQService() {
        const serv = {
            login: function (param) {
                param.url = lpt.baseApiUrl + '/qq/login/' + param.param.code;
                return lpt.post(param);
            }
        }
        lpt.qqServ = serv;
    }

    function initPostService() {
        const serv = {
            getDefaultPost: function (id) {
                return {
                    isDefault: true,
                    id: id,
                    creator: {},
                    rights: {
                        update_category: false,
                        edit: false,
                        create: false,
                        delete: false
                    },
                    editable: false,
                    liked_by_viewer: false,
                    forward_cnt: 0,
                    comment_cnt: 0,
                    like_cnt: 0,
                    type_str: undefined,
                    full_text_id: undefined,
                    category_path: [],
                    settled: false
                };
            },
            queryForCategory: wrap(function (param) {
                const queryType = param.param.queryType || 'popular';
                param.url = lpt.baseApiUrl + '/post/' + queryType;
                return param.querior.query(param);
            }),
            queryForCreator: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/post/creator';
                return param.querior.query(param);
            }),
            getFullText: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/post/full_text/' + param.param.fullTextId;
                return lpt.get(param);
            }),
            updateContent: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/post/content';
                return lpt.patch(param);
            }),
            create: wrap(function (param) {
                const type = param.param.type || 'public';
                param.url = lpt.baseApiUrl + '/post/' + type;
                return lpt.post(param);
            }),
            get: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/post/' + param.param.postId;
                return lpt.get(param);
            }),
            setTopComment: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/post/top_comment/' + (param.param.isCancel ? 'cancel' : 'create');
                return lpt.patch(param);
            }),
            setCategory: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/post/category';
                return lpt.patch(param);
            }),
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
            getDefaultCommentL1: function (id) {
                return {
                    id: id,
                    ...defaultComment,
                    poster_rep_cnt: 0,
                    post_id: undefined,
                    l2_cnt: 0
                };
            },
            getDefaultCommentL2: function (id) {
                return {
                    id: id,
                    l1_id: undefined,
                    ...defaultComment
                };
            },
            level1: 'l1',
            level2: 'l2',
            get: wrap(function (param) {
                param.url = `${lpt.baseApiUrl}/comment/${param.param.type}/${param.param.commentId}`;
                return lpt.get(param);
            }),
            query: wrap(function (param) {
                param.url = `${lpt.baseApiUrl}/comment/${param.param.type}/${param.param.queryType}`;
                return param.querior.query(param);
            }),
            create: wrap(function (param) {
                param.url = `${lpt.baseApiUrl}/comment/${param.param.type}`;
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
                    param.url = lpt.baseApiUrl + '/post';
                } else if (param.param.type == 'cml1') {
                    param.url = lpt.baseApiUrl + '/comment/l1';
                } else if (param.param.type == 'cml2') {
                    param.url = lpt.baseApiUrl + '/comment/l2';
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
            list.sort((c1, c2) => {
                return c2.disp_seq - c1.disp_seq
            });
        }

        const adminLevel = {
            // 发布可修改帖子，需要本目录权限
            create_editable_post: 1
        };

        const serv = {
            setAdminRights: function (category) {
                if (!category.rights)
                    return;
                const rights = category.rights;
                const thisLevel = rights.this_level;
                if (typeof thisLevel !== 'undefined') {
                    if (thisLevel > adminLevel.create_editable_post)
                        rights.create_editable_post = true;
                }
            },
            getDefaultCategory: function (id) {
                return {
                    isDefault: true,
                    parent_id: -1,
                    id: id,
                    name: '',
                    top_post_id: -1,
                    sub_list: undefined,
                    cover_img: '',
                    icon_img: '',
                    allow_post_level: undefined,
                    rights: {
                        this_level: 0,
                        parent_level: 0,
                        update_info: false,
                        update_disp_seq: false,
                        update_parent: false,
                        create_editable_post: false,
                        update_allow_post_level: false,
                        talk_ban: false
                    },
                    path_list: [],
                    disp_seq: 0,
                    allow_user_post: undefined,
                    is_leaf: true
                }
            },
            groundCategoryId: 1,
            rootCategoryId: 1,
            setInfo: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/info';
                return lpt.patch(param);
            }),
            setTopPost: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/top_post/' + (param.param.isCancel ? 'cancel' : 'create');
                return lpt.patch(param);
            }),
            setAllowPostLevel: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/allow_post_level/' + (param.param.isCancel ? 'cancel' : 'create');
                return lpt.patch(param);
            }),
            setCacheNum: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/cache_num';
                return lpt.patch(param);
            }),
            setDispSeq: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/disp_seq';
                return lpt.patch(param);
            }),
            setDefaultSub: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/def_sub';
                return lpt.patch(param);
            }),
            setParent: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/parent';
                return lpt.patch(param);
            }),
            get: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/' + param.param.id;
                param.appendSuccess(result => processList(result.object.sub_list), true);
                return lpt.get(param)
            }),
            create: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category';
                return lpt.post(param);
            }),
            delete: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/';
                return lpt.delete(param);
            }),
            reload: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/reload';
                return lpt.post(param);
            }),
            getRoots: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/roots';
                param.appendSuccess(result => processList(result.object), true);
                return lpt.get(param);
            }),
            getDirectSub: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/category/direct_sub/' + param.data.categoryId;
                param.appendSuccess(result => processList(result.object), true);
                return lpt.get(param)
            }),
            getPathStr: function (list) {
                let pathStr = '';
                for (let i = 0; i < list.length; ++i)
                    pathStr += list[i].name + '\\';
                return pathStr;
            }
        };
        lpt.categoryServ = serv;
    }

    function initFollowService() {
        const serv = {
            queryFollower: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/follow/follower';
                return param.querior.query(param);
            }),
            getFollowing: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/follow/following/' + param.param.userId;
                return lpt.get(param);
            }),
            follow: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/follow';
                return lpt.post(param);
            }),
            unFollow: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/follow';
                return lpt.delete(param);
            })
        };
        lpt.followServ = serv;
    }

    function initForwardService() {
        const serv = {
            query: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/forward/list';
                return param.querior.query(param);
            }),
            create: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/forward';
                return lpt.post(param);
            })
        };
        lpt.forwardServ = serv;
    }

    function initLikeService() {
        const serv = {
            query: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/like/list/' + param.param.type;
                return param.querior.query(param);
            }),
            like: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/like/' + param.param.type;
                return lpt.post(param);
            }),
            unlike: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/like/' + param.param.type;
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
                param.url = lpt.baseApiUrl + '/notice';
                return param.querior.query(param);
            }),
            markAsRead: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/notice/read';
                return lpt.post(param);
            })
        };
        lpt.noticeServ = serv;
    }

    function initPermissionService() {
        const serv = {
            getForCategory: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/permission/category/' + param.param.categoryId;
                return lpt.get(param);
            }),
            getForUser: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/permission/user/' + param.param.userId;
                return lpt.get(param);
            }),
            setAdmin: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/permission';
                return lpt.post(param);
            }),
            delAdmin: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/permission';
                return lpt.delete(param);
            }),
            updateAdmin: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/permission';
                return lpt.patch(param);
            })
        };
        lpt.permissionServ = serv;
    }

    function initNewsService() {
        const serv = {
            query: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/news';
                return param.querior.query(param);
            })
        };
        lpt.newsServ = serv;
    }

    function initSearchService() {
        const serv = {
            search: wrap(function (param) {
                param.url = `${lpt.baseApiUrl}/search/${param.param.searchType}/${param.param.words}/${param.param.mode}`;
                return lpt.get(param);
            })
        };
        lpt.searchServ = serv;
    }

    function initCorrectService() {
        const serv = {
            correct: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/correct_data/' + param.param.type;
                param.responseType = 'text';
                param.useResultFormat = false;
                return lpt.post(param);
            }),
            flush: wrap(function (param) {
                param.url = lpt.baseApiUrl + '/correct_data/flush';
                param.responseType = 'text';
                param.useResultFormat = false;
                return lpt.post(param);
            })
        }
        lpt.correctServ = serv;
    }

    return lpt;
}

export default _initLaputa();