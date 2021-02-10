<template>
	<a-form id="form" ref="form" layout="horizontal" :model="form"
	        :label-col="{span: 8}" :wrapper-col="{span: 16}" :rules="rules"
	        hide-required-mark="">
		<a-form-item ref="username" name="username">
			<template v-slot:label>
				<label class="label">用户名：</label>
			</template>
			<a-input v-model:value="form.username" placeholder="请输入"/>
		</a-form-item>
		<a-form-item ref="password" name="password">
			<template v-slot:label>
				<label class="label">密码：</label>
			</template>
			<a-input-password v-model:value="form.password" placeholder="请输入"/>
		</a-form-item>
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
import lpt from '@/lib/js/laputa/laputa';
import md5 from 'crypto-js/md5';
import {notification} from 'ant-design-vue';
import {Toast} from 'vant';
import {makeError} from '@/lib/js/uitls/form-util';
import global from '@/lib/js/global';

export default {
	name: 'SignIn',
	data() {
		return {
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
	},
	methods: {
		backToHome() {
			this.$router.push({path: '/home'});
		},
		toSignUp() {
			this.$router.push({path: '/sign_up'});
		},
		signIn() {
			const ref = this;
			const form = this.$refs.form;
			form.validate().then(() => {
				return lpt.operatorServ.signIn({
					consumer: ref.lptConsumer,
					data: {
						nick_name: ref.form.username,
						password: md5(ref.form.password).toString()
					},
					success() {
						global.events.emit('login');
						ref.backToHome();
						notification.open({
							message: '登录成功',
							duration: 1
						});
					},
					fail(result) {
						if (result.error_code === 1010130207) {
							// 用户名不存在
							makeError(ref.$refs.username, result.message);
						} else if (result.error_code === 1010130209) {
							// 密码错误
							makeError(ref.$refs.password, result.message);
						} else {
							// 其他错误
							Toast.fail(result.message);
						}
					}
				})
			});
		}
	}
}
</script>

<style scoped>
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
</style>