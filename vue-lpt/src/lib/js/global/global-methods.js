import remHelper from '@/lib/js/uitls/rem-helper';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import events from './global-events';

const option = {
    moduleTitle: ''
};

const state = {};

const mentionParseReg = /(@\S+ )/g;

function parseOption(opt) {
    if (opt.moduleTitle) {
        document.title = opt.moduleTitle;
    }
}

export default {
    setOption(_option) {
        Object.assign(option, _option);
        parseOption(_option);
    },
    prompt() {
        // 由BlogApp.vue和LptApp.vue中定义具体的prompt实现
    },
    setTitle(param) {
        let title = '';
        let tabTitle = '';
        if (param.insertBefore) {
            title = param.insertBefore + '-' + state.curTitle;
        } else if (param.appendAfter) {
            title = state.curTitle + '-' + param.appendAfter;
        } else {
            if (param.contentDesc) {
                title += param.contentDesc + '-';
                tabTitle += param.contentDesc + '-';
            }
            if (param.pageDesc) {
                title += param.pageDesc + '-';
                tabTitle += param.pageDesc;
            }
            title += option.moduleTitle;
        }
        state.curTitle = title;
        document.title = title;
        if (param.route && param.route.meta) {
            events.emit('onSetBlogTitle', {
                path: param.route.meta.fullMainPath,
                title: tabTitle
            });
        }
    },
    parsePxSize(str) {
        if (str.indexOf('rem') > 0) {
            return remHelper.remToPx(parseFloat(str));
        } else {
            return parseFloat(str);
        }
    },
    parseContentElement(opt) {
        const html = opt.el.innerHTML;
        opt.el.innerHTML = html.replaceAll(mentionParseReg, '<a class="m-link" href="#">$1</a>');
        const mentionLinks = opt.el.getElementsByClassName('m-link');
        if (mentionLinks) {
            for (let i = 0; i < mentionLinks.length; ++i) {
                const mElem = mentionLinks[i];
                let name = mElem.innerText;
                name = name.substring(1, name.length - 1);
                mElem.addEventListener('click', (event) => {
                    lpt.userServ.getByName({
                        consumer: this.lptConsumer,
                        throwError: true,
                        param: {
                            userName: name
                        },
                        success: (result) => {
                            opt.router.push({
                                path: '/user_home_page/' + result.object.id
                            });
                        },
                        fail: (result) => {
                            Toast.fail(result.message);
                        }
                    }).catch(() => {
                        Toast.fail('查无此人');
                    });
                    event.stopPropagation();
                    event.preventDefault();
                });
            }
        }
    }
}