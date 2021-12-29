<template>
	<div class="main-area" :style="{height: scrollHeight, position: 'relative'}" keep-scroll-top>
		<a-back-top :style="{bottom: (mainBarHeight + 10) + 'px'}" :target="getElement"/>
		<post-list ref="postList" :post-of="'news'" :item-style="postItemStyle"/>
	</div>
</template>

<script>
import PostList from '@/components/post/post_list/PostList';
import global from '@/lib/js/global';
import {toRef} from 'vue';

export default {
	name: 'News',
	components: {
		PostList
	},
	data() {
		return {
			mainBarHeight: toRef(global.states.style, 'tabBarHeight')
		}
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	computed: {
		scrollHeight() {
			const mainViewHeight = global.states.style.bodyHeight;
			// 底部高度加0.5的padding
			const barHeight = this.mainBarHeight;
			return mainViewHeight - barHeight + 'px';
		}
	},
	activated() {
		if (this.lptContainer !== 'blogDrawer') {
			global.methods.setTitle({
				pageDesc: '动态',
				route: this.$route
			});
		}
	},
	methods: {
		getElement() {
			return this.$el;
		},
		postItemStyle(post, index) {
			const style = {};
			if (index === 0) {
				style.marginTop = '0.5rem';
			}
			return style;
		}
	}
}
</script>

<style scoped>
.main-area {
	overflow-y: scroll;
}

.main-area::-webkit-scrollbar {
	display: none;
}

:global(.van-pull-refresh__head) {
	background-color: #ececec;
}
</style>