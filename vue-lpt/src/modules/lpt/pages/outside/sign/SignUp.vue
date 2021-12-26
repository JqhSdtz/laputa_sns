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
		<a-form-item ref="extra" name="extra">
			<template v-slot:label>
				<label class="label">辅助文字：</label>
			</template>
			<a-input v-model:value="form.extra"/>
			<p style="font-size: 0.85rem; margin: 0; line-height: 1rem;">
				*不保存，无意义，仅用于工作量证明
			</p>
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
		<p style="text-align: center; word-break: break-all; width: 90%; margin-left: 5%; font-size: 0.9rem;">
			*此方式注册目前无找回密码功能，普通用户建议直接使用QQ登录
		</p>
		<a-spin v-show="isWorkProofing" style="display:block; margin: 0 auto" tip="正在执行工作量证明"/>
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
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import md5 from 'crypto-js/md5';
import Base64 from 'crypto-js/enc-base64';
import UTF8 from 'crypto-js/enc-utf8';
import {Dialog, Toast} from 'vant';

export default {
	name: 'SignUp',
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	inject: {
		prompts: {
			type: Object
		}
	},
	data() {
		const ref = this;
		return {
			isWorkProofing: false,
			form: {
				username: '',
				password: '',
				extra: '000',
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
						pattern: /^[^@#$<>()\[\]\\\.,;:\s"]{2,40}$/,
						message: '用户名长度应在2-40之间，且不能包含@#$<>()\[\]\\\.,;:\"以及空格'
					},
					{
						trigger: 'blur',
						validator(rule, value) {
							if (!value)
								return Promise.reject('请输入用户名');
							return lpt.userServ.checkName({
								consumer: ref.lptConsumer,
								throwError: true,
								param: {
									userName: value
								}
							});
						}
					}
				],
				extra: [
					{
						required: true,
						message: '请输入辅助文字'
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
			this.$router.push({path: '/home/mine'});
		},
		toSignIn() {
			this.$router.push({path: '/sign_in'});
		},
		signUp() {
			this.$refs.form.validate().then(() => {
				const ref = this;
				return new Promise((resolve) => {
					this.prompts.confirm({
						title: '将执行工作量证明',
						message: '为避免直接注册用户过多，注册时采用工作量证明的方式，简单来说，需要您等待浏览器几分钟到十几分钟的时间',
						onConfirm: () => {
							this.isWorkProofing = true;
							setTimeout(() => {
								const name64 = Base64.stringify(UTF8.parse(ref.form.username));
								const extra64 = Base64.stringify(UTF8.parse(ref.form.extra));
								const nameMd5 = md5(name64).toString();
								let calRes, rand, count = 0;
								const countLimit = 10000000000;
								const checkHead = () => calRes.substring(0, 6) === '000000';
								console.log('开始时间:' + new Date());
								do {
									rand = (++count).toString().padStart(10, '0');
									calRes = md5(nameMd5 + extra64 + rand).toString();
								} while (!checkHead() && count < countLimit);
								this.isWorkProofing = false;
								if (!checkHead()) {
									Toast.fail('工作量证明失败，请修改辅助文字后再次尝试');
								}
								console.log('名称MD5:' + nameMd5);
								console.log('辅助文字Base64:' + extra64);
								console.log('随机数:' + rand);
								console.log('计算结果:' + calRes);
								console.log('结束时间:' + new Date());
								resolve(lpt.operatorServ.signUp({
									consumer: ref.lptConsumer,
									param: {
										extra64,
										rand,
										calRes
									},
									data: {
										nick_name: ref.form.username,
										password: md5(ref.form.password).toString()
									},
									success() {
										global.events.emit('signUp');
										ref.backToHome();
									},
									fail(result) {
										Toast.fail(result.message);
									}
								}));
							}, 100);
						}
					});
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
	top: 50%;
}
</style>