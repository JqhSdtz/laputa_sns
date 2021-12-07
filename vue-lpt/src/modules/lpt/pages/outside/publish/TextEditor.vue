<template>
    <div>
        <van-nav-bar ref="navBar" :title="title" left-text="返回" left-arrow @click="$emit('close')"/>
        <van-field type="textarea" :maxlength="maxlength"
            v-model="curText"
            autofocus
            show-word-limit
            :autosize="{minHeight: maxHeight, maxHeight: maxHeight}"
            :placeholder="placeholder || '请输入内容'"/>
    </div>
</template>

<script>
import global from '@/lib/js/global';
import remHelper from '@/lib/js/uitls/rem-helper';

export default {
    name: 'TextEditor',
    props: {
        title: String,
        maxlength: [String, Number],
        placeholder: String,
        text: String
    },
    emits: ['update:text', 'close'],
    data() {
        return {
            curText: this.text
        }
    },
    computed: {
        maxHeight() {
            // 总高度减去顶部导航栏高度，40是来自padding
            return global.states.style.bodyHeight - remHelper.remToPx(3) - 40;
        }
	},
    watch: {
        curText(newValue) {
            this.$emit('update:text', newValue);
        },
        text(newValue) {
            this.curText = newValue;
        }
    }
}
</script>

<style scoped>
:global(:root) {
    --van-nav-bar-height: 3rem;
}
</style>