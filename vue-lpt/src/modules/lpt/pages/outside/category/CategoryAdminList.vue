<template>
	<div class="main-area" :style="{height: scrollHeight, position: 'relative'}">
		<van-pull-refresh v-model="isRefreshing" @refresh="onPullRefresh"
		                  success-text="刷新成功" style="height: 100%">
			<div class="user-list">
				<div v-for="obj in list" :key="obj.id">
					<div>
						<span style="display: inline-block; width: 75%;">
							<user-item :user="obj.user" :show-id="true" style="padding: 1rem 1rem;"/>
						</span>
						<span style="width: 25%">等级:{{ obj.level }}</span>
					</div>
					<div v-if="category.rights.parent_level > obj.level" style="margin-left: 1rem; text-align: right">
						<a-button type="link" @click="updateAdmin(obj)">修改</a-button>
						<a-button type="link" @click="deleteAdmin(obj)">删除</a-button>
					</div>
				</div>
			</div>
		</van-pull-refresh>
	</div>
</template>

<script>
import UserItem from '@/modules/lpt/pages/outside/user/user_list/item/UserItem';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import global from '@/lib/js/global';

export default {
	name: 'CategoryAdminList',
	props: {
		categoryId: String
	},
	components: {
		UserItem
	},
	data() {
		const category = global.states.categoryManager.get({
			itemId: this.categoryId
		});
		return {
			category,
			hasEverLoad: false,
			list: [],
			isRefreshing: false
		}
	},
	computed: {
		scrollHeight() {
			const mainViewHeight = global.states.style.bodyHeight;
			return mainViewHeight + 'px';
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.loadMore();
		if (this.category.isDefault) {
			lpt.categoryServ.get({
				param: {
					id: this.categoryId
				},
				success: (result) => {
					global.states.categoryManager.add(result.object);
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		}
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
		},
		updateAdmin(permObj) {
			const prompt = global.methods.prompt;
			prompt({
				title: '设置管理等级',
				placeholder: '设置该用户管理等级（不能大于自身父目录管理等级）',
				onValidate: (value) => {
					const intValue = parseInt(value);
					if (isNaN((intValue))) {
						return '请输入数字';
					} else {
						const parentLevel = this.category.rights.parent_level;
						if (intValue > parentLevel) {
							return '设置管理等级不能高于自身父目录管理等级';
						} else if (permObj.level === parentLevel && intValue < permObj.level) {
							return '不能给和操作者同级的用户设置更低的等级';
						}
					}
					return true;
				},
				onConfirm: (value) => {
					return prompt({
						onConfirm: (opComment) => {
							lpt.permissionServ.updateAdmin({
								consumer: this.lptConsumer,
								data: {
									category_id: this.category.id,
									user_id: permObj.user.id,
									level: value,
									op_comment: opComment
								},
								success: () => {
									permObj.level = value;
									Toast.success('设置管理员等级成功');
								},
								fail(result) {
									Toast.fail(result.message);
								}
							});
						}
					});
				}
			});
		},
		deleteAdmin(permObj) {
			const prompt = global.methods.prompt;
			prompt({
				onConfirm: (opComment) => {
					lpt.permissionServ.delAdmin({
						consumer: this.lptConsumer,
						data: {
							category_id: this.category.id,
							user_id: permObj.user.id,
							op_comment: opComment
						},
						success: () => {
							this.list.splice(this.list.indexOf(permObj), 1);
							Toast.success('删除管理员成功');
						},
						fail(result) {
							Toast.fail(result.message);
						}
					});
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

.main-area {
	height: 100%;
	overflow-y: scroll;
}

.main-area::-webkit-scrollbar {
	display: none;
}
</style>