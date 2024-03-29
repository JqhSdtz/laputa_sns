<template>
	<div style="height: 100%; padding-top: 2rem">
		<a-upload-m
			ref="uploader"
			accept="image/*"
			:action="uploadUrl"
			:before-upload="beforeUpload"
			:showUploadList="false"
			:headers="uploadHeader"
			:width="clientWidth + 'px'"
			@change="handleChange">
		</a-upload-m>
		<div style="text-align: center" @click="selectUpload">
			<img class="ava" :src="myAvatarUrl"/>
			<p style="color: rgb(150,150,150)">
				点击修改
			</p>
		</div>
		<a-button style="width: 50%; margin-left: 25%" @click="saveUserInfo">
			保存
		</a-button>
		<a-form id="form" ref="form" :model="form"  :label-col="{span: 8}"
		        :wrapper-col="{span: 16}" :rules="rules"
		        style="margin-top: 1rem"
		        hide-required-mark="">
			<a-form-item ref="nick_name" name="nick_name">
				<template v-slot:label>
					<label class="label">用户名</label>
				</template>
				<a-input class="no-border-input" v-model:value="form.nick_name" placeholder="请输入"/>
			</a-form-item>
			<a-form-item ref="email" name="email">
				<template v-slot:label>
					<label class="label">邮箱</label>
				</template>
				<a-input class="no-border-input" v-model:value="form.email" placeholder="请输入"/>
			</a-form-item>
			<a-form-item ref="phone" name="phone">
				<template v-slot:label>
					<label class="label">手机</label>
				</template>
				<a-input class="no-border-input" v-model:value="form.phone" placeholder="请输入"/>
			</a-form-item>
			<a-form-item ref="sch_info" name="sch_info">
				<template v-slot:label>
					<label class="label">学校信息</label>
				</template>
				<a-input class="no-border-input" v-model:value="form.sch_info" placeholder="请输入"/>
			</a-form-item>
			<a-form-item ref="intro" name="intro">
				<template v-slot:label>
					<label class="label">个人介绍</label>
				</template>
				<a-textarea rows="5" v-model:value="form.intro" placeholder="请输入"/>
			</a-form-item>
		</a-form>
	</div>
</template>

<script>
import AUploadM from '@/components/upload/Upload';
import {Toast} from 'vant';
import lpt from "@/lib/js/laputa/laputa";
import global from "@/lib/js/global";

export default {
	name: 'ModUserInfo',
	components: {
		AUploadM
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	created() {
		const ref = this;
		this.lptConsumer = lpt.createConsumer();
		const curOperator = lpt.operatorServ.getCurrent();
		this.oriUserName = curOperator.user.nick_name;
		lpt.userServ.getInfo({
			consumer: this.lptConsumer,
			param: {
				userId: curOperator.user.id
			},
			success(result) {
				ref.form = result.object;
			},
			fail(result) {
				Toast.fail(result.message);
			}
		});
	},
	data() {
		const ref = this;
		this.uploadUrl = lpt.baseApiUrl + '/oss/ava';
		return {
			me: global.states.curOperator,
			uploadHeader: {
				'x-lpt-user-token': ''
			},
			form: lpt.operatorServ.getCurrent(),
			rules: {
				nick_name: [
					{
						required: true,
						message: '请输入用户名'
					},
					{
						trigger: 'blur',
						pattern: /^[^@#$<>()\[\]\\\.,;:\s"]{2,12}$/,
						message: '用户名长度应在2-12之间，且不能包含@#$<>()\[\]\\\.,;:\"以及空格'
					},
					{
						trigger: 'blur',
						validator(rule, value) {
							if (!value) {
								return Promise.reject('请输入用户名');
							} if (value === ref.oriUserName) {
								// 用户名没变，则直接返回成功
								return Promise.resolve();
							} else {
								return lpt.userServ.checkName({
									consumer: lpt.createConsumer(),
									ignoreGlobalBusyChange: true,
									param: {
										userName: value
									}
								});
							}
						}
					}
				],
				email: {
					type: 'email',
					trigger: 'blur',
					message: '邮箱格式不正确'
				},
				phone: {
					pattern: /^1[0-9]{10}$/,
					trigger: 'blur',
					message: '手机号格式不正确'
				},
				sch_info: {
					max: 20,
					trigger: 'blur',
					message: '学校信息不能超过20个字'
				},
				intro: {
					max: 200,
					trigger: 'blur',
					message: '个人介绍不能超过200个字'
				}
			}
		}
	},
	computed: {
		myAvatarUrl() {
			return lpt.getUserAvatarUrl(this.me.user);
		},
		clientWidth() {
			if (this.lptContainer === 'blogDrawer') {
				return global.states.style.drawerWidth;
			} else if (this.lptContainer === 'blogMain') {
				return global.states.style.blogMainWidth;
			} else {
				return global.states.style.bodyWidth;
			}
		},
	},
	methods: {
		selectUpload() {
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
						&& resultUrl.indexOf('ava') >= 0) {
					this.me.user.raw_avatar = info.file.response.object;
					this.form.raw_avatar = this.me.user.raw_avatar;
				} else {
					Toast.fail('上传失败');
					console.error(info);
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
		saveUserInfo() {
			this.$refs.form.validate().then(() => {
				if (this.form.nick_name !== this.oriUserName) {
					// 修改用户名
					lpt.userServ.updateUserName({
						consumer: lpt.createConsumer(),
						data: {
							nick_name: this.form.nick_name
						},
						success: () => {
							this.oriUserName = this.form.nick_name;
						},
						fail(result) {
							Toast.fail('用户名修改失败，失败原因:' + result.message);
						}
					});
				}
				return lpt.userServ.setInfo({
					consumer: this.lptConsumer,
					data: this.form,
					success: () => {
						Toast.success('修改成功');
						global.states.userManager.add(this.me.user);
					},
					fail(result) {
						Toast.fail(result.message);
					}
				});
			}).catch(() => {});
		}
	}
}
</script>

<style scoped>
.ava {
	border-radius: 100%;
	width: 5rem;
	height: 5rem;
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
