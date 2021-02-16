<template>
	<div id="main-area" :style="{height: scrollHeight, position: 'relative'}" keep-scroll-top>
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
	computed: {
		scrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			// 底部高度加0.5的padding
			const barHeight = this.mainBarHeight;
			return mainViewHeight - barHeight + 'px';
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
#main-area {
	overflow-y: scroll;
}

#main-area::-webkit-scrollbar {
	display: none;
}
</style>