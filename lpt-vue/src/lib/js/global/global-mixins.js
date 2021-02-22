// 用于给元素添加事件时去重
import {watch} from 'vue';

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

// 用于切换页面时保持原页面的滚动条位置
const keepScrollTop = {
    props: {
        fillParent: Object,
        keepScrollTop: Boolean
    },
    data() {
        return {
            forceReloadFlag: true
        }
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
    mounted() {
        // 因为deactivated钩子中scrollTop值已清零，所以要在每次变化时都记录下当前的scrollTop
        this.$nextTick(() => {
            if (this.keepScrollTop || (this.$el.hasAttribute
                && this.$el.hasAttribute('keep-scroll-top'))
                && isDistinct('keep-scroll-top', this.$el)) {
                this._keepScrollTop = true;
                this.scrollListener = () => {
                    this.curScrollTop = this.$el.scrollTop;
                };
                this.$el.addEventListener('scroll', this.scrollListener);
            }
        });
    },
    activated() {
        if (this._keepScrollTop) {
            this.$el.scrollTop = this.curScrollTop || 0;
        }
    },
    beforeUnmount() {
        if (this._keepScrollTop) {
            this.$el.removeEventListener('scroll', this.scrollListener);
            removeDistinct('keep-scroll-top', this.$el);
        }
    },
    methods: {
        forceReload() {
            this.forceReloadFlag = false;
            this.$nextTick(() => {
                this.forceReloadFlag = true;
            });
        }
    }
}

export default [keepScrollTop];