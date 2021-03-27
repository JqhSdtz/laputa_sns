<template>
	<van-tabs v-model:active="curTabKey" swipeable lazy-render :style="{width: clientWidth + 'px'}">
		<van-tab name="post" title="帖子">
			<post-item class="post-item" v-for="post in postList" :post-id="post.id" :key="post.id"/>
		</van-tab>
		<van-tab name="user" title="用户">
			<user-item class="user-item" v-for="user in userList" :key="user.id" :user="user"/>
		</van-tab>
		<van-tab name="category" title="目录">
			<div v-for="category in categoryList" :key="category.id">
				<p style="text-align: center">{{category.name}}</p>
			</div>
		</van-tab>
	</van-tabs>
</template>

<script>
import PostItem from '@/components/post/item/PostItem';
import UserItem from '@/modules/lpt/pages/outside/user/user_list/item/UserItem';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import global from "@/lib/js/global";

export default {
	name: 'SearchIndex',
	components: {
		PostItem,
		UserItem
	},
	data() {
		return {
			curTabKey: 'post',
			postList: [],
			categoryList: [],
			userList: []
		}
	},
	watch: {
		curTabKey() {
			this.init();
		}
	},
	computed: {
		clientWidth() {
			if (global.vars.env === 'blog') {
				return global.states.style.drawerWidth;
			} else {
				return document.body.clientWidth;
			}
		},
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.init();
	},
	methods: {
		init() {
			const ref = this;
			const query = this.$route.query;
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
						ref.userList = result.object;
					} else if (ref.curTabKey === 'category') {
						result.object = result.object.filter((obj) => obj !== null);
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