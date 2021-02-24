<template>
	<div id="main-area" :style="{height: scrollHeight, position: 'relative'}">
		<van-pull-refresh v-model="isRefreshing" @refresh="onPullRefresh"
		                  success-text="刷新成功" style="height: 100%">
			<div class="user-list">
				<div v-for="obj in list" :key="obj.id">
					<span style="display: inline-block; width: 75%;">
						<user-item :user="obj.user" style="padding: 1rem 1rem;"/>
					</span>
					<span style="width: 25%">等级:{{ obj.level }}</span>
				</div>
			</div>
		</van-pull-refresh>
	</div>
</template>

<script>
import UserItem from '@/pages/outside/user/user_list/item/UserItem';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from "vant";

export default {
	name: 'CategoryAdminList',
	props: {
		categoryId: String
	},
	components: {
		UserItem
	},
	data() {
		return {
			hasEverLoad: false,
			list: [],
			isRefreshing: false
		}
	},
	computed: {
		scrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			return mainViewHeight + 'px';
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.loadMore();
	},
	methods: {
		onPullRefresh() {
			this.reset();
			this.loadMore();
		},
		reset() {
			this.hasEverLoad = false;
		},
		loadMore() {
			const ref = this;
			lpt.permissionServ.getForCategory({
				consumer: this.lptConsumer,
				param: {
					categoryId: parseInt(this.categoryId)
				},
				success(result) {
					if (!ref.hasEverLoad) {
						ref.list = result.object;
						ref.hasEverLoad = true;
					} else {
						ref.list = ref.list.concat(result.object);
					}
				},
				fail(result) {
					Toast.fail(result.message);
				},
				complete() {
					ref.isRefreshing = false;
				}
			});
		}
	}
}
</script>

<style scoped>
.user-list {
	height: 100%;
	overflow-y: visible;
}

#main-area {
	height: 100%;
	overflow-y: scroll;
}

#main-area::-webkit-scrollbar {
	display: none;
}
</style>