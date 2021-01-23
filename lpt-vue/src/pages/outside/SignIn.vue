<template>
	<a-form id="form" ref="form" layout="horizontal" :model="form"
	        :label-col="{span: 8}" :wrapper-col="{span: 16}" :rules="rules"
	        hide-required-mark="">
		<a-form-item name="username">
			<template v-slot:label>
				<label class="label">用户名：</label>
			</template>
			<a-input v-model:value="form.username" placeholder="请输入" @focus="onFocus"/>
		</a-form-item>
		<a-form-item name="password">
			<template v-slot:label>
				<label class="label">密码：</label>
			</template>
			<a-input-password v-model:value="form.password" placeholder="请输入" @focus="onFocus"/>
		</a-form-item>
		<a-row id="message" v-if="signFailed">
			<a-col span="16" offset="8">
				<p class="message-text">{{ failMessage }}</p>
			</a-col>
		</a-row>
		<a-form-item :wrapperCol="{span: 24}">
			<a-button style="width: 100%;" type="primary" @click="signIn">
				登录
			</a-button>
		</a-form-item>
		<a-form-item :wrapperCol="{span: 24}">
			<a-button style="width: 45%" @click="backToHome">
				返回
			</a-button>
			<a-button style="width: 45%; margin-left: 10%" @click="toSignUp">
				注册
			</a-button>
		</a-form-item>
	</a-form>
</template>

<script>
import lpt from '@/lib/js/laputa'
import md5 from 'crypto-js/md5'
import {notification} from "ant-design-vue";

export default {
	name: 'SignIn',
	inject: {
		setGlobalBusy: {
			type: Function
		}
	},
	data() {
		return {
			signFailed: false,
			failMessage: '',
			form: {
				username: '',
				password: '',
			},
			rules: {
				username: {
					required: true,
					message: '请输入用户名'
				},
				password: {
					required: true,
					message: '请输入密码'
				}
			}
		};
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		const ref = this;
		this.lptConsumer.onBusyChange(isBusy => {
			this.$nextTick(() => {
				ref.setGlobalBusy(isBusy);
			});
		});
	},
	methods: {
		backToHome() {
			this.$router.push({path: '/home'});
		},
		toSignUp() {
			this.$router.push({path: '/sign_up'});
		},
		onFocus() {
			this.signFailed = false;
		},
		signIn() {
			const ref = this;
			this.$refs.form.validate().then(() => {
				lpt.operatorServ.signIn({
					consumer: ref.lptConsumer,
					data: {
						nick_name: ref.form.username,
						password: md5(ref.form.password).toString()
					},
					success() {
						ref.signFailed = false;
						ref.backToHome();
						notification.open({
							message: '登录成功',
							duration: 1
						});
					},
					fail(result) {
						ref.signFailed = true;
						ref.failMessage = result.message;
					}
				})
			}).catch(() => {
				this.signFailed = false;
			});
		}
	}
}
</script>

<style scoped lang="less">
@import (reference) '~ant-design-vue/dist/antd.less';

.label {
	line-height: 2.5rem;
}

#form {
	position: absolute;
	width: 75%;
	transform: translate(-50%, -50%);
	left: 50%;
	top: 40%;
}

#message {
	margin-top: -1rem;
}

.message-text {
	color: @error-color
}
</style>