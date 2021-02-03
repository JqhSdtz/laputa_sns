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
		<!-- 经过尝试，confirmPassword必须名称保持一致 -->
		<a-form-item ref="confirmPassword" name="confirmPassword">
			<template v-slot:label>
				<label class="label">确认密码：</label>
			</template>
			<a-input-password v-model:value="form.confirmPassword" placeholder="请输入"/>
		</a-form-item>
		<a-form-item :wrapperCol="{span: 24}">
			<a-button style="width: 100%;" type="primary" @click="signUp">
				注册
			</a-button>
		</a-form-item>
		<a-form-item :wrapperCol="{span: 24}">
			<a-button style="width: 45%" @click="backToHome">
				返回
			</a-button>
			<a-button style="width: 45%; margin-left: 10%" @click="toSignIn">
				登录
			</a-button>
		</a-form-item>
	</a-form>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";
import md5 from "crypto-js/md5";
import {notification, message} from 'ant-design-vue';

export default {
	name: 'SignUp',
	inject: {
		refreshMainView: {
			type: Function
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	data() {
		const ref = this;
		return {
			form: {
				username: '',
				password: '',
				confirmPassword: ''
			},
			rules: {
				username: [
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
							return lpt.userServ.checkName({
								consumer: ref.lptConsumer,
								param: {
									userName: value
								}
							});
						}
					}
				],
				password: [
					{
						required: true,
						message: '请输入密码'
					},
					{
						trigger: 'blur',
						min: 6,
						message: '密码长度应大于6'
					}
				],
				confirmPassword: [
					{
						required: true,
						message: '请输入确认密码'
					},
					{
						trigger: 'blur',
						validator(rule, value) {
							if (value === ref.form.password) {
								return Promise.resolve();
							} else {
								return Promise.reject('确认密码不一致');
							}
						}
					}
				]
			}
		};
	},
	methods: {
		backToHome() {
			this.$router.push({path: '/home'});
		},
		toSignIn() {
			this.$router.push({path: '/sign_in'});
		},
		signUp() {
			this.$refs.form.validate().then(() => {
				const ref = this;
				return lpt.operatorServ.signUp({
					consumer: ref.lptConsumer,
					data: {
						nick_name: ref.form.username,
						password: md5(ref.form.password).toString()
					},
					success() {
						this.refreshMainView();
						ref.backToHome();
						notification.success({
							message: '注册成功',
							description: '快来发现新世界！',
							duration: 1.5
						});
					},
					fail(result) {
						message.warn(result.message);
					}
				});
			})
		}
	}
}
</script>

<style scoped lang="less">
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