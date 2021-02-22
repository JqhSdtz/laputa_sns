<template>
	<div style="height: 100%; padding-top: 1rem; background-color: rgb(245, 245, 245)">
		<van-cell v-if="category.rights.update_info" class="cell" title="修改信息" is-link :to="'/mod_category_info/' + category.id"/>
		<van-cell v-if="category.rights.update_disp_seq" class="cell" title="修改排序号" is-link @click="changeDispSeq"/>
	</div>
</template>

<script>
import global from '@/lib/js/global';
import lpt from "@/lib/js/laputa/laputa";
import {Toast} from "vant";

export default {
	name: 'CategorySetting',
	props: {
		categoryId: String
	},
	data() {
		const category = global.states.categoryManager.get(this.categoryId);
		return {
			category
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		lpt.categoryServ.get({
			consumer: this.lptConsumer,
			param: {
				id: this.categoryId
			},
			success(result) {
				global.states.categoryManager.add(result.object);
			},
			fail(result) {
				Toast.fail(result.message);
			}
		});
	},
	methods: {
		changeDispSeq() {
			const param = {
				title: '请输入排序号',
				placeholder: '数字越大越靠前，最大99999',
				onValidate(value) {
					const intValue = parseInt(value);
					if (isNaN((intValue))) {
						return '请输入数字';
					} else if (intValue > 99999) {
						return '排序号不能大于99999';
					}
					return true;
				},
				onConfirm(value) {
					console.log(value);
				}
			};
			global.methods.prompt(param);
		}
	}
}
</script>

<style scoped>
.cell {
	margin-top: 1rem;
}
</style>