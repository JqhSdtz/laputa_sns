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

// 用于切换页面时保持原页面的滚动条位置
const keepScrollTop = {
    mounted() {
        // 因为deactivated钩子中scrollTop值已清零，所以要在每次变化时都记录下当前的scrollTop
        this.$nextTick(() => {
            if (this.$el.hasAttribute
                    && this.$el.hasAttribute('keep-scroll-top')
                    && isDistinct('keep-scroll-top', this.$el)) {
                this.keepScrollTop = true;
                this.scrollListener = () => {
                    this.curScrollTop = this.$el.scrollTop;
                };
                this.$el.addEventListener('scroll', this.scrollListener);
            }
        });
    },
    activated() {
        if (this.keepScrollTop) {
            this.$el.scrollTop = this.curScrollTop || 0;
        }
    },
    beforeUnmount() {
        if (this.keepScrollTop) {
            this.$el.removeEventListener('scroll', this.scrollListener);
            removeDistinct('keep-scroll-top', this.$el);
        }
    }
}

export default [keepScrollTop];