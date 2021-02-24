<template>
	<div v-if="hasLoaded" style="height: 100%; padding-top: 1rem; background-color: rgb(245, 245, 245)">
		<van-cell class="cell" title="查看管理员列表" is-link :to="'/category_admin_list/' + category.id"/>
		<van-cell v-if="category.rights.update_info" class="cell" title="修改信息" is-link
		          :to="'/mod_category_info/' + category.id"/>
		<van-cell v-if="category.rights.update_disp_seq" class="cell" title="查看/修改排序号" is-link @click="changeDispSeq"/>
		<van-cell v-if="category.rights.parent_level > 1" class="cell" title="设置管理员" is-link @click="setAdmin"/>
	</div>
	<prompt-dialog ref="prompt">
		<template v-if="showSelectedUser" v-slot:tip>
			<user-item :show-id="true" :user="curSelectedUser"/>
		</template>
	</prompt-dialog>
</template>

<script>
import global from '@/lib/js/global';
import lpt from "@/lib/js/laputa/laputa";
import {Toast} from "vant";
import PromptDialog from "@/components/global/PromptDialog";
import UserItem from '@/components/user/item/UserItem';

export default {
	name: 'CategorySetting',
	components: {
		UserItem,
		PromptDialog
	},
	props: {
		categoryId: String
	},
	data() {
		const category = global.states.categoryManager.get(this.categoryId);
		return {
			hasLoaded: false,
			showSelectedUser: false,
			curSelectedUser: lpt.userServ.getDefaultUser(-1),
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
			success: (result) => {
				this.hasLoaded = true;
				global.states.categoryManager.add(result.object);
			},
			fail(result) {
				Toast.fail(result.message);
			}
		});
	},
	methods: {
		changeDispSeq() {
			// 修改排序号是父目录权限，能修改该目录的排序号，必然也能修改同级其他目录的排序号
			const prompt = global.methods.prompt;
			prompt({
				title: '当前目录排序号',
				focusTitle: '请输入0~99999的数字',
				value: this.category.disp_seq,
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
				onConfirm: (value) => {
					return prompt({
						onConfirm: (opComment) => {
							lpt.categoryServ.setDispSeq({
								consumer: this.lptConsumer,
								data: {
									id: this.category.id,
									disp_seq: parseInt(value),
									op_comment: opComment
								},
								success: () => {
									this.category.disp_seq = value;
									Toast.success('修改排序号成功');
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
		setAdmin() {
			const prompt = this.$refs.prompt.prompt;
			prompt({
				title: '输入目标用户的用户名',
				placeholder: ' ',
				onValidate: (value) => {
					return lpt.userServ.getByName({
						consumer: this.lptConsumer,
						throwError: true,
						param: {
							userName: value
						},
						success: (result) => {
							this.curSelectedUser = result.object;
						}
					}).catch(() => {
						return Promise.reject(`用户"${value}"不存在`);
					});
				},
				onConfirm: () => {
					this.showSelectedUser = true;
					return prompt({
						title: '设置管理等级',
						placeholder: '设置该用户管理等级（不能大于自身父目录管理等级）',
						onValidate: (value) => {
							const intValue = parseInt(value);
							if (isNaN((intValue))) {
								return '请输入数字';
							} else if (intValue > this.category.rights.parent_level) {
								return '设置管理等级不能高于自身父目录管理等级';
							}
							return true;
						},
						onFinish: () => this.$nextTick(() => this.showSelectedUser = false),
						onConfirm: (value) => {
							console.log(value);
						}
					});
				}
			});
		}
	}
}
</script>

<style scoped>
.cell {
	margin-top: 1rem;
}
</style>