<template>
	<div class="main-area" :style="{height: scrollHeight, position: 'relative'}" keep-scroll-top>
		<a-back-top :style="{bottom: (mainBarHeight + 10) + 'px'}" :target="getElement"/>
		<post-list ref="postList" :post-of="'news'"/>
	</div>
</template>

<script>
import PostList from '@/components/post/post_list/PostList';
import global from '@/lib/js/global';

export default {
	name: 'News',
	components: {
		PostList
	},
	data() {
		return {
			mainBarHeight: global.vars.style.tabBarHeight
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
	created() {
		if (this.lptConsumer !== 'blogDrawer') {
			global.methods.setTitle({
				pageDesc: '动态'
			});
		}
	},
	methods: {
		getElement() {
			return this.$el;
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
</style>