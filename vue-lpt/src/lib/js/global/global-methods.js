import remHelper from '@/lib/js/uitls/rem-helper';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import events from './global-events';
import states from './global-states';

const option = {
    moduleTitle: ''
};

const state = {};

const mentionParseReg = /(@\S+ )/g;
const galleryContentReg = /描述：([\s\S]*)\n封面：([\s\S]*)\n张数：([\s\S]*)/;

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
    },
    checkPostIsGalleryItem(post) {
        // 检查帖子是否是相册目录中的帖子
        return galleryContentReg.test(post.content);
    },
    parseGalleryItemContent(post) {
        const res = galleryContentReg.exec(post.content);
        if (!res || res.length < 4) {
            console.warn('图片贴解析错误，错误帖子对象如下：');
            console.warn(post);
            return null;
        }
        post.customContent = this.desc;
        post.parsedImages = [];
        return {
            desc: res[1],
            coverUrl: res[2],
            photoNum: parseInt(res[3])
        }
    },
    parseGalleryItemFullText(post, fullText) {
        const parts = fullText.split(/\-+分割线\-+/);
        let imgListStr = '';
        if (parts.length > 1) {
            imgListStr = parts[parts.length - 1].trim();
            post.customFullText = parts[0];
        } else {
            imgListStr = fullText;
        }
        const list = imgListStr.split('\n');
        const images = [];
        list.forEach((img) => {
            const parts = img.split(';');
            if (parts.length === 0) return;
            const imgItem = {
                src: parts[0],
                thumb: parts.length > 1 ? parts[1] : undefined
            };
            images.push(imgItem);
        });
        return images;
    }
}