<template>
	<div class="main-area">
		<a-upload-m
			ref="uploader"
			accept="image/*"
			:action="uploadUrl"
			:before-upload="beforeUpload"
			:showUploadList="false"
			:headers="uploadHeader"
			@change="handleChange">
		</a-upload-m>
		<div @click="uploadCoverImg">
			<img style="width: 100%;" :src="coverImgUrl"/>
			<p style="text-align: right; margin-right: 1rem; color: rgb(150,150,150)">
				点击修改封面
			</p>
		</div>
		<div style="text-align: center">
			<div style="display: inline-block" @click="uploadIconImg">
				<img style="width: 5rem; height: 5rem" :src="iconImgUrl"/>
				<p style="color: rgb(150,150,150)">
					点击修改图标
				</p>
			</div>
			<div v-if="opType === 'create'" style="display: inline-block; margin-left: 2rem">
				<p style="margin-bottom: 0.5rem; font-size: 1.25rem;">公开</p>
				<van-switch v-model="form.isPublic" size="24" style="margin-bottom: 1.25rem"/>
			</div>
		</div>
		<a-button style="width: 50%; margin-left: 25%" @click="saveCategoryInfo">
			{{ opType === 'create' ? '创建' : '保存' }}
		</a-button>
		<a-form id="form" ref="form" :model="form" :label-col="{span: 8}"
		        :wrapper-col="{span: 16}" :rules="rules"
		        style="margin-top: 1rem"
		        hide-required-mark="">
			<a-form-item ref="name" name="name">
				<template v-slot:label>
					<label class="label">目录名</label>
				</template>
				<a-input class="no-border-input" v-model:value="form.name" placeholder="请输入"/>
			</a-form-item>
			<a-form-item ref="intro" name="intro">
				<template v-slot:label>
					<label class="label">目录介绍</label>
				</template>
				<a-textarea rows="5" v-model:value="form.intro" placeholder="请输入"/>
			</a-form-item>
		</a-form>
	</div>
</template>

<script>
import AUploadM from '@/components/upload/Upload';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import global from '@/lib/js/global';

export default {
	name: 'ModCategoryInfo',
	components: {
		AUploadM
	},
	props: {
		categoryId: String
	},
	data() {
		const opType = this.$route.query.opType;
		let category;
		if (opType === 'create') {
			category = lpt.categoryServ.getDefaultCategory(-1);
			category.parent_id = this.$route.query.parentId;
		} else {
			category = global.states.categoryManager.get({
				itemId: this.categoryId,
				fail(result) {
					Toast.fail(result.message);
				}
			});
		}
		category.isPublic = true;
		return {
			opType,
			category,
			uploadUrl: lpt.baseApiUrl + '/oss/cat',
			uploadHeader: {
				'x-lpt-user-token': ''
			},
			form: category,
			rules: {
				name: [
					{
						required: true,
						message: '请输入目录名'
					},
					{
						trigger: 'blur',
						pattern: /^[^@#$<>()\[\]\\\.,;:\s"]{2,12}$/,
						message: '目录名长度应在2-12之间，且不能包含@#$<>()\[\]\\\.,;:\"以及空格'
					}
				],
				intro: {
					max: 200,
					trigger: 'blur',
					message: '个人介绍不能超过200个字'
				}
			}
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	computed: {
		iconImgUrl() {
			return lpt.getCategoryIconUrl(this.category);
		},
		coverImgUrl() {
			return lpt.getCategoryCoverUrl(this.category);
		}
	},
	methods: {
		uploadIconImg() {
			this.uploadType = 'icon';
			// 触发上传组件的点击事件
			this.$refs.uploader.$el.click();
		},
		uploadCoverImg() {
			this.uploadType = 'cover';
			// 触发上传组件的点击事件
			this.$refs.uploader.$el.click();
		},
		handleChange(info) {
			if (!this.isUploading && info.file.status === 'uploading') {
				this.isUploading = true;
				global.events.emit('pushGlobalBusy', {
					isBusy: true
				});
			} else if (info.file.status === 'done') {
				this.isUploading = false;
				global.events.emit('pushGlobalBusy', {
					isBusy: false
				});
				const resultUrl = info.file.response.object;
				if (typeof resultUrl === 'string'
					&& resultUrl.indexOf('cat') >= 0) {
					if (this.uploadType === 'cover') {
						this.category.cover_img = info.file.response.object;
					} else if (this.uploadType === 'icon') {
						this.category.icon_img = info.file.response.object;
					}
				} else {
					Toast.fail('上传失败');
					console.warn(info);
				}
			} else if (info.file.status === 'error') {
				this.isUploading = false;
				global.events.emit('pushGlobalBusy', {
					isBusy: false
				});
				Toast.fail('上传失败');
				console.error(info);
			}
		},
		beforeUpload(file) {
			if (file.type.indexOf('image') < 0) {
				Toast.fail('只能上传图片文件');
				return false;
			}
			if (file.size > 10485760) {
				Toast.fail('上传图片不能大于10M');
				return false;
			}
			this.uploadHeader["x-lpt-user-token"] = lpt.getCurUserToken();
		},
		saveCategoryInfo() {
			// 目录设置置顶贴需要输入理由
			this.$refs.form.validate().then(() => {
				global.methods.prompt({
					onConfirm: (value) => {
						const isCreate = this.opType === 'create';
						if (isCreate) {
							this.category.parent = {
								id: this.category.parent_id
							};
							this.category.type = this.category.isPublic ? 0 : 1;
						}
						const fun = isCreate ? lpt.categoryServ.create : lpt.categoryServ.setInfo;
						fun({
							consumer: this.lptConsumer,
							data: {
								...this.form,
								op_comment: value
							},
							success: (result) => {
								Toast.success(isCreate ? '创建成功' : '修改成功');
								if (isCreate) {
									this.category.id = parseInt(result.object);
									this.opType = 'update';
									const parent = global.states.categoryManager.get({
										itemId: this.category.parent_id
									});
									if (parent && parent.sub_list) {
										parent.sub_list = parent.sub_list.push(this.category);
									}
								}
								Object.assign(this.form, this.category);
							},
							fail(result) {
								Toast.fail(result.message);
							}
						});
					}
				});
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

.label {
	line-height: 2.5rem;
	margin-right: 1rem;
}

#form {
	width: 80%;
	margin-left: 10%;
}

.no-border-input {
	border-top: none;
	border-left: none;
	border-right: none;
}

:global(.ant-form-item-label) {
	text-align: right !important;
	padding: 0.15rem 0 0 !important;
}
</style>