import description from "@/modules/blog/description";

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
        if (param.insertBefore) {
            title = param.insertBefore + '-' + state.curTitle;
        } else if (param.appendAfter) {
            title = state.curTitle + '-' + param.appendAfter;
        } else {
            if (param.contentDesc) title += param.contentDesc + '-';
            if (param.pageDesc) title += param.pageDesc + '-';
            title += option.moduleTitle;
        }
        state.curTitle = title;
        document.title = title;
    }
}