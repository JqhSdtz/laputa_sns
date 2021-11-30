// 用于给元素添加事件时去重
const distinctMap = new Map();

function isDistinct(setKey, key) {
    let distinctSet = distinctMap.get(setKey);
    if (!distinctSet) {
        distinctSet = new Set();
        distinctSet.add(key);
        distinctMap.set(setKey, distinctSet);
        return true;
    }
    if (distinctSet.has(key))
        return false;
    distinctSet.add(key);
    return true;
}

function removeDistinct(setKey, key) {
    if (distinctMap.has(setKey)) {
        distinctMap.get(setKey).delete(key);
    }
}

// 用于页面强制刷新
const forceReload = {
    data() {
        return {
            forceReloadFlag: true
        }
    },
    methods: {
        forceReload(opt) {
            this.forceReloadFlag = false;
            this.$nextTick(() => {
                this.forceReloadFlag = true;
                this.$nextTick(() => {
                    opt?.afterReload && opt.afterReload();
                });
            });
        }
    }
};

// 用于切换页面时保持原页面的滚动条位置
const scrollTopMap = new Map();
const keepScrollTop = {
    mounted() {
        // 因为deactivated钩子中scrollTop值已清零，所以要在每次变化时都记录下当前的scrollTop
        this.$nextTick(() => {
            if (this.keepScrollTop || (this.$el.hasAttribute
                && this.$el.hasAttribute('keep-scroll-top'))
                && isDistinct('keep-scroll-top', this.$el)) {
                this._keepScrollTop = true;
                this._scrollListener = () => {
                    this.curScrollTop = this.$el.scrollTop;
                };
                this.$el.addEventListener('scroll', this.scrollListener);
            }
        });
    },
    activated() {
        if (this._keepScrollTop) {
            this.$el.scrollTop = this.curScrollTop;
        } else if (this._keepScrollTopId) {
            const scrollTop = scrollTopMap.get(this._keepScrollTopId);
            scrollTop.el.scrollTop = scrollTop.offset || 0;
        }
    },
    beforeUnmount() {
        if (this._keepScrollTop) {
            this.$el.removeEventListener('scroll', this._scrollListener);
            removeDistinct('keep-scroll-top', this.$el);
        } else if (this._keepScrollTopId) {
            const scrollTop = scrollTopMap.get(this._keepScrollTopId);
            scrollTop.el.removeEventListener('scroll', scrollTop.listener);
            removeDistinct('keep-scroll-top', scrollTop.el);
        }
    },
    methods: {
        bindScrollTop(option) {
            const el = option.el || this.$el;
            const id = option.id || this.name;
            const scrollTop = {
                el,
                id,
                offset: 0
            };
            const listener = () => {
                scrollTop.offset = el.scrollTop;
            };
            el.addEventListener('scroll', listener);
            scrollTop.listener = listener;
            scrollTopMap.set(id, scrollTop);
            this._keepScrollTopId = id;
        }
    }
};

// 用户list组件在长度不够时填满父元素
const fillParent = {
    props: {
        fillParent: Object
    },
    watch: {
        finished(isFinished) {
            // 列表加载完后如果高度达不到父元素的高度，则将列表高度设为父元素的高度
            if (!isFinished)
                return;
            const parent = this.fillParent;
            if (this.fillParent && isDistinct('fill-parent', this.$el)) {
                const ref = this;
                this.$nextTick(() => {
                    if (ref.$el.clientHeight < parent.clientHeight) {
                        ref.$el.style.height = parent.clientHeight + 'px';
                    }
                });
            }
        }
    },
    methods: {
        fillParentElem(parent) {
            this.$nextTick(() => {
                if (this.$el.clientHeight < parent.clientHeight) {
                    this.$el.style.height = parent.clientHeight + 'px';
                }
            });
        }
    }
};

export default [forceReload, keepScrollTop, fillParent];