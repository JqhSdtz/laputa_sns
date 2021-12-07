<template>
	<van-tabs ref="tabs" v-model:active="curTabKey" swipeable lazy-render :style="{width: clientWidth + 'px'}">
		<van-tab ref="postTab" name="post" title="帖子">
			<post-list :post-list="postList"/>
		</van-tab>
		<van-tab ref="userTab" name="user" title="用户">
			<user-item class="user-item" v-for="user in userList" :key="user.id" :user="user"/>
		</van-tab>
		<van-tab ref="categoryTab" name="category" title="目录">
			<div v-for="category in categoryList" :key="category.id">
				<category-grid-item :category-id="category.id"/>
			</div>
		</van-tab>
	</van-tabs>
</template>

<script>
import CategoryGridItem from '@/components/category/item/CategoryGridItem';
import PostList from '@/components/post/post_list/PostList';
import UserItem from '@/modules/lpt/pages/outside/user/user_list/item/UserItem';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import global from "@/lib/js/global";

export default {
	name: 'SearchIndex',
	components: {
		PostList,
		UserItem,
		CategoryGridItem
	},
	data() {
		return {
			curTabKey: 'post',
			postList: [],
			categoryList: [],
			userList: [],
			preHref: ''
		}
	},
	watch: {
		curTabKey() {
			this.init();
		},
		$route(route) {
			if (route.name && route.name.indexOf('searchIndex') >= 0) {
				this.onActivated();
			}
		},
	},
	computed: {
		clientWidth() {
			const curTab = this.$refs[this.curTabKey + 'Tab'];
			if (curTab) {
				this.$nextTick(() => curTab.$parent.resize());
				this.$refs.tabs.resize();
			}
			if (global.vars.env === 'blog') {
				return global.states.style.drawerWidth;
			} else {
				return global.states.style.bodyWidth;
			}
		},
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.init();
	},
	activated() {
		this.onActivated();
	},
	methods: {
		onActivated() {
			const href = this.$route.fullPath;
			if (href !== this.preHref) {
				this.init();
			}
		},
		init() {
			const ref = this;
			const query = this.$route.query;
			this.preHref = this.$route.fullPath;
			if (!query.value || !query.mode) {
				// 直接刷新的页面会导致没有query的值，这个时候不加载
				return;
			}
			lpt.searchServ.search({
				consumer: this.lptConsumer,
				param : {
					searchType: this.curTabKey,
					words: query.value,
					mode: query.mode,
				},
				success(result) {
					if (ref.curTabKey === 'post') {
						global.states.postManager.addList(result.object);
						ref.postList = result.object;
					} else if (ref.curTabKey === 'user') {
						global.states.userManager.addList(result.object);
						ref.userList = result.object;
					} else if (ref.curTabKey === 'category') {
						result.object = result.object.filter((obj) => obj !== null);
						global.states.categoryManager.addList(result.object);
						ref.categoryList = result.object;
					}
				},
				fail(result) {
					Toast.fail(result.message);
				},
			});
		}
	}
}
</script>

<style scoped>

</style>