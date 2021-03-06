<template>
	<div id="main-area">
		<van-cell v-for="permission in list" :key="permission.id" style="margin-top: 1.5rem">
			<span>{{getPathStr(permission.category_path)}}</span>
			<span style="float:right; margin-right: 1rem">等级：{{permission.level}}</span>
		</van-cell>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';

export default {
	name: 'PermissionList',
	props: {
		userId: String
	},
	data() {
		return {
			list: []
		}
	},
	created() {
		const ref = this;
		this.lptConsumer = lpt.createConsumer();
		lpt.permissionServ.getForUser({
			consumer: this.lptConsumer,
			param: {
				userId: this.userId
			},
			success(result) {
				ref.list = result.object;
			},
			fail(result) {
				Toast.fail(result.message);
			}
		});
	},
	methods: {
		getPathStr(pathList) {
			return lpt.categoryServ.getPathStr(pathList);
		}
	}
}
</script>

<style scoped>
#main-area {
	height: 100%;
	overflow-y: scroll;
}

#main-area::-webkit-scrollbar {
	display: none;
}
</style>