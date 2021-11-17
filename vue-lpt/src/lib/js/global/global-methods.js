import remHelper from '@/lib/js/uitls/rem-helper';
import events from './global-events';

const option = {
    moduleTitle: ''
};

const state = {};

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
    }
}