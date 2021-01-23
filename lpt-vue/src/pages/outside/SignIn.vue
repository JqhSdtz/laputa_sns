<template>
	<a-form id="form" layout="horizontal" :model="form"
	        :label-col="{span: 8}" :wrapperCol="{span: 16}">
		<a-form-item>
			<template v-slot:label>
				<label class="label">用户名：</label>
			</template>
			<a-input v-model:value="form.userName" placeholder="请输入"/>
		</a-form-item>
		<a-form-item>
			<template v-slot:label>
				<label class="label">密码：</label>
			</template>
			<a-input v-model:value="form.password" placeholder="请输入"/>
		</a-form-item>
		<a-row id="message" v-if="signFailed">
			<a-col span="16" offset="8">
				<p class="message-text">{{ failMessage }}</p>
			</a-col>
		</a-row>
		<a-form-item>
			<a-button type="primary" @click="signIn">
				登录
			</a-button>
		</a-form-item>
	</a-form>
	<!--	<a-button @click="backToHome">返回</a-button>-->
</template>

<script>
import lpt from '@/lib/js/laputa'
import md5 from 'crypto-js/md5'

export default {
	name: 'SignIn',
	data() {
		return {
			signFailed: false,
			failMessage: '',
			form: {
				userName: '',
				password: '',
			},
		};
	},
	activated() {
		this.lptConsumer = Symbol();
	},
	methods: {
		backToHome() {
			this.$router.push({path: '/home'});
		},
		signIn() {
			const ref = this;
			lpt.operatorServ.signIn({
				consumer: ref.lptConsumer,
				data: {
					nick_name: ref.form.userName,
					password: md5(ref.form.password).toString()
				},
				success() {
					ref.backToHome();
				},
				fail(result) {
					ref.signFailed = true;
					ref.failMessage = result.message;
				}
			})
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