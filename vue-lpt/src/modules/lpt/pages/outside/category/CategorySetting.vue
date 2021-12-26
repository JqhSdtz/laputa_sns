<template>
	<div class="main-area" style="height: 100%; padding-top: 1rem; background-color: rgb(245, 245, 245)">
		<van-cell class="cell" title="目录ID" :value="categoryId"/>
		<van-cell v-if="me.isAdmin" class="cell" title="查看管理员列表" is-link :to="'/category_admin_list/' + category.id"/>
		<van-cell v-if="category.rights.update_info" class="cell" title="修改信息" is-link
		          :to="'/mod_category_info/' + category.id"/>
		<van-cell v-if="category.rights.update_disp_seq" class="cell" title="查看/修改排序号" is-link @click="changeDispSeq"/>
		<van-cell v-if="category.rights.parent_level > 1" class="cell" title="添加管理员" is-link @click="setAdmin"/>
		<van-cell v-if="category.rights.update_allow_post_level" class="cell" title="设置允许发帖管理等级" is-link
		          @click="setAllowPostLevel"/>
		<!-- 设置/取消禁言仅在根目录的设置页中显示，因为在后端中限定了只有根目录才会有该权限 -->
		<van-cell v-if="category.rights.talk_ban" class="cell" title="设置/取消禁言" is-link @click="setTalkBan"/>
		<van-cell v-if="category.rights.update_parent" class="cell" title="迁移父目录" is-link @click="changeParent"/>
		<van-cell v-if="category.rights.create" class="cell" title="创建子目录" is-link @click="showCreatePage"/>
		<van-cell v-if="category.rights.delete" class="cell" title="删除目录" is-link @click="deleteCategory"/>
	</div>
</template>

<script>
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from "vant";

export default {
	name: 'CategorySetting',
	props: {
		categoryId: String
	},
	inject: {
		lptContainer: {
			type: String
		},
		prompts: {
            type: Object
        }
	},
	data() {
		const category = global.states.categoryManager.get({
			itemId: this.categoryId,
			fail(result) {
				Toast.fail(result.message);
			}
		});
		return {
			me: global.states.curOperator,
			category
		}
	},
	watch: {
		categoryId(newCategoryId) {
			this.category = global.states.categoryManager.get({
				itemId: newCategoryId,
				fail(result) {
					Toast.fail(result.message);
				}
			});
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	methods: {
		changeDispSeq() {
			// 修改排序号是父目录权限，能修改该目录的排序号，必然也能修改同级其他目录的排序号
			const prompt = this.prompts.plainPrompt;
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
			this.prompts.userPrompt((user, plainPrompt) => {
				return {
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
					onConfirm: (value) => {
						return plainPrompt({
							onConfirm: (opComment) => {
								lpt.permissionServ.setAdmin({
									consumer: this.lptConsumer,
									data: {
										category_id: this.category.id,
										user_id: user.id,
										level: value,
										op_comment: opComment
									},
									success: () => {
										Toast.success('添加管理员成功');
									},
									fail(result) {
										Toast.fail(result.message);
									}
								});
							}
						});
					}
				}
			});
		},
		setAllowPostLevel() {
			const prompt = this.prompts.plainPrompt;
			prompt({
				title: '当前允许发帖管理等级',
				focusTitle: '请输入0~99的数字',
				value: this.category.allow_post_level,
				placeholder: '空白表示取消发帖限制',
				onValidate(value) {
					if (!value)
						return true;
					const intValue = parseInt(value);
					if (isNaN((intValue))) {
						return '请输入数字';
					} else if (intValue > 99) {
						return '管理等级不能大于99';
					}
					return true;
				},
				onConfirm: (value) => {
					return prompt({
						onConfirm: (opComment) => {
							lpt.categoryServ.setAllowPostLevel({
								consumer: this.lptConsumer,
								param: {
									isCancel: !value
								},
								data: {
									id: this.category.id,
									allow_post_level: value ? parseInt(value) : undefined,
									op_comment: opComment
								},
								success: () => {
									this.category.allow_post_level = value;
									Toast.success('允许发帖管理等级修改成功');
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
		changeParent() {
			this.prompts.categoryPrompt(category => {
				return {
					onConfirm: (opComment) => {
						lpt.categoryServ.setParent({
							consumer: this.lptConsumer,
							data: {
								id: this.category.id,
								parent_id: category.id,
								op_comment: opComment
							},
							success: () => {
								Toast.success('迁移父目录成功');
							},
							fail(result) {
								Toast.fail(result.message);
							}
						});
					}
				}
			});
		},
		setTalkBan() {
			this.prompts.userPrompt((user, plainPrompt) => {
				return {
					title: '设置禁言截止时间',
					inputType: 'datePicker',
					value: new Date(),
					tipMessage: '选择当前时间即取消禁言',
					onValidate: () => true,
					onConfirm: (value) => {
						return plainPrompt({
							onConfirm: (opComment) => {
								lpt.userServ.setTalkBan({
									consumer: this.lptConsumer,
									data: {
										id: user.id,
										talk_ban_to: value,
										op_comment: opComment
									},
									success: () => {
										Toast.success('设置禁言成功');
									},
									fail(result) {
										Toast.fail(result.message);
									}
								});
							}
						});
					}
				}
			});
		},
		deleteCategory() {
			const prompt = this.prompts.plainPrompt;
			prompt({
				onConfirm: (opComment) => {
					lpt.categoryServ.delete({
						consumer: this.lptConsumer,
						data: {
							id: this.category.id,
							op_comment: opComment
						},
						success: () => {
							const parent = global.states.categoryManager.get({
								itemId: this.category.parent_id
							});
							if (parent && parent.sub_list) {
								parent.sub_list = parent.sub_list.filter(sub => sub.id !== this.category.id);
							}
							this.$router.go(-2);
							Toast.success('删除目录成功');
						},
						fail: (result) => {
							Toast.fail(result.message);
						}
					});
				}
			});
		},
		showCreatePage() {
			this.$router.push({
				path: '/mod_category_info',
				query: {
					opType: 'create',
					parentId: this.categoryId
				}
			});
		}
	}
}
</script>

<style scoped>
.main-area {
	height: 100%;
	overflow-y: scroll;
}

.main-area::-webkit-scrollbar {
	display: none;
}

.cell {
	margin-top: 1rem;
}
</style>