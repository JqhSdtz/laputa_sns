<template>
	<div style="display: inline-block">
		<span @click="showCategorySetting">
			<setting-outlined v-if="category.hasRights" class="setting-icon"/>
			<info-circle-outlined v-else class="setting-icon"/>
		</span>
		<span v-if="typeof category.pinned !== 'undefined'" @click="changePin">
			<pushpin-outlined v-if="!category.pinned" class="icon"/>
			<pushpin-filled v-else class="icon" :rotate="-45"/>
		</span>
		<span @click="showCategoryTypeTip">
			<link-outlined v-if="category.type === 0 || category.type === 2" class="icon"/>
			<disconnect-outlined v-else-if="category.type === 1" class="icon"/>
		</span>
	</div>
</template>

<script>
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';
import {Dialog, Toast} from 'vant';
import {
	SettingOutlined, PushpinOutlined, PushpinFilled,
	LinkOutlined, DisconnectOutlined, InfoCircleOutlined
} from '@ant-design/icons-vue';

const categoryTypeStr = ['公开目录', '私有目录', '公开目录'];

export default {
	name: 'ActionBar',
	props: {
		categoryId: Number
	},
	components: {
		SettingOutlined,
		PushpinOutlined,
		PushpinFilled,
		LinkOutlined,
		DisconnectOutlined,
		InfoCircleOutlined
	},
	inject: {
		prompts: {
			type: Object
		}
	},
	data() {
		const category = global.states.categoryManager.get({
			itemId: this.categoryId
		});
		return {
			category
		}
	},
	methods: {
		changePin() {
			const isCancel = this.category.pinned;
			const fun = isCancel ? lpt.userServ.unpinRecentVisit
				: lpt.userServ.pinRecentVisit;
			fun({
				param: {
					categoryId: this.categoryId
				},
				success: () => {
					this.category.pinned = !isCancel;
					Toast.success(isCancel ? '取消固定成功' : '固定到最近访问成功');
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		},
		showCategorySetting() {
			this.$router.push({
				path: '/category_setting/' + this.categoryId
			});
		},
		showCategoryTypeTip() {
			const type = this.category.type;
			const typeStr = categoryTypeStr[type];
			let message = '';
			if (type === 0 || type === 2) {
				message = '该目录是公开目录，这意味着该目录的内容在其上级目录中也会显示，并参与上级目录的排序。';
			} else if (type === 1) {
				message = '该目录是私有目录，这意味着该目录的内容不会在其上级目录中显示，即只能在本页面中查看。';
			}
			this.prompts.alert({
				title: typeStr,
				message: message,
			});
		}
	}
}
</script>

<style scoped>
.icon {
	float: right;
	font-size: 1.5rem;
	margin-right: 1rem;
}

.setting-icon {
	float: right;
	margin-right: 1rem;
	font-size: 1.5rem;
}
</style>