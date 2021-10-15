<template>
	<div class="main-area">
		<a-row v-if="hasSigned" style="padding-top: 2rem;">
			<a-col span="5" offset="2">
				<img class="ava" :src="myAvatarUrl"/>
			</a-col>
			<a-col span="16" offset="1">
				<div class="name">
					<span>{{ me.user.nick_name }}</span>
					<span style="margin-left: 1rem">ID: {{ me.user.id }}</span>
				</div>
			</a-col>
		</a-row>
		<a-row v-if="hasSigned" justify="center">
			<a-col class="cnt-bar-item" span="7" @click="showFollowingList">
				关注:{{ me.user.following_cnt }}
			</a-col>
			<a-col class="cnt-bar-item" span="8" @click="showFollowersList">
				粉丝:{{ me.user.followers_cnt }}
			</a-col>
			<a-col class="cnt-bar-item" span="7">
				发帖:{{ me.user.post_cnt }}
			</a-col>
		</a-row>
		<div v-if="hasSigned" style="margin-top: 1.5rem">
			<van-cell title="个人主页" is-link :to="'/user_home_page/' + me.user.id"/>
			<van-cell is-link to="/notice_list">
				<template #title>
					<span class="custom-title">通知</span>
					<a-badge style="margin-left: 1rem; margin-top: -0.5rem;" :count="me.unread_notice_cnt"/>
				</template>
			</van-cell>
			<van-cell title="个人信息" is-link to="/mod_user_info"/>
			<van-cell title="设置/修改密码" is-link @click="changePassword"/>
			<van-cell v-if="me.isAdmin" title="管理权限" is-link :to="'/permission_list/' + me.user.id"/>
			<van-cell v-if="isSuperAdmin" title="查看数据库统计信息" is-link to="/druid_stat"/>
			<van-cell v-if="isSuperAdmin" title="校正数据" is-link @click="correctData"/>
			<van-cell v-if="isSuperAdmin" title="刷新缓存数据" is-link @click="flushData"/>
			<van-cell v-if="isSuperAdmin" title="重载目录" is-link @click="reloadCategory"/>
			<van-cell v-if="env === 'blog'" title="完整功能请访问社区" is-link to="/lpt_qr_code"/>
			<van-cell title="注销" is-link @click="signOut"/>
		</div>
		<div v-if="!hasSigned && !isLogging" style="width: 90%; margin-left: 5%">
			<img style="display: block; height: 18rem; margin: 2rem auto;" :src="qqSymbolImg" @click="openQQLogin"/>
			<!--			<img style="margin-top: 1rem; width: 100%;" :src="qqButtonImg" @click="openQQLogin"/>-->
			<a-button type="primary" style="margin-top: 1rem; width: 100%; height: 3.5rem; font-size: 1.7rem"
			          @click="openQQLogin">
				使用QQ帐号登录
			</a-button>
			<a-button type="primary" style="margin-top: 1rem; width: 100%; height: 3rem; font-size: 1.5rem"
			          @click="signIn">
				其他方式登录
			</a-button>
		</div>
		<div v-if="isLogging" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%)">
			<a-spin size="large" tip="正在登陆……"/>
		</div>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
// import qqButtonImg from '@/assets/qq/Connect_logo_5.png';
import qqSymbolImg from '@/assets/qq/qq_symbol.png';
import {Dialog, Toast} from 'vant';
import md5 from 'crypto-js/md5';

export default {
	name: 'Mine',
	data() {
		return {
			// qqButtonImg,
			env: global.vars.env,
			qqSymbolImg,
			qqLoginUrl: '',
			showQQLogin: false,
			isLogging: false,
			me: global.states.curOperator
		}
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		const token = lpt.util.getCookie('token');
		let userId;
		if (global.states.hasSigned.value) {
			userId = this.me.user.id;
		} else if (token && token.indexOf('@') > 0) {
			userId = token.split('@')[0];
		}
		if (userId) {
			lpt.userServ.get({
				consumer: this.lptConsumer,
				param: {
					userId: userId
				},
				success(result) {
					global.states.curOperator.user = result.object;
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		}
		this.checkQQLogin();
	},
	activated() {
		if (this.lptContainer !== 'blogDrawer') {
			global.methods.setTitle({
				pageDesc: '我的'
			});
		}
	},
	computed: {
		isSuperAdmin() {
			const permissionMap = this.me.permission_map;
			return permissionMap && permissionMap[lpt.categoryServ.groundCategoryId] == 99;
		},
		hasSigned() {
			return global.states.hasSigned.value;
		},
		myAvatarUrl() {
			return lpt.getUserAvatarUrl(this.me.user);
		}
	},
	methods: {
		showFollowersList() {
			this.$router.push({
				path: '/followers_list/' + this.me.user.id.toString()
			});
		},
		showFollowingList() {
			this.$router.push({
				path: '/following_list/' + this.me.user.id.toString()
			});
		},
		signIn() {
			this.$router.push({path: '/sign_in'});
		},
		signOut() {
			Dialog.confirm({
				message: '确认注销？',
				closeOnClickOverlay: true
			}).then(() => {
				lpt.operatorServ.signOut({
					consumer: this.lptConsumer,
					success() {
						global.events.emit('signOut');
						Toast.success('注销成功');
					},
					fail(result) {
						Toast.fail(result.message);
					}
				}).catch(() => {
				});
			}).catch(() => {
			});
		},
		changePassword() {
			const prompt = global.methods.prompt;
			prompt({
				title: '修改密码',
				focusTitle: '请输入密码',
				placeholder: '请输入6位以上的密码',
				inputType: 'password',
				onValidate(password) {
					if (password.length < 6) {
						return '密码长度应大于6'
					}
					return true;
				},
				onConfirm: (password) => {
					return prompt({
						title: '确认密码',
						focusTitle: '请重复输入密码',
						placeholder: '请重复上一步输入的密码',
						inputType: 'password',
						onValidate(confirm) {
							if (confirm !== password) {
								return '两次输入的密码不一致'
							}
							return true;
						},
						onConfirm: () => {
							lpt.userServ.updatePassword({
								data: {
									password: md5(password).toString()
								},
								success: () => {
									Toast.success('修改密码成功');
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
		checkQQLogin() {
			const query = this.$route.query;
			if (!query.code)
				return;
			const state = localStorage.getItem('curQQLoginState');
			if (!state || query.state !== state)
				return;
			this.isLogging = true;
			lpt.qqServ.login({
				param: {
					code: query.code
				},
				success: (result) => {
					const operator = result.object;
					if (operator.from_register) {
						global.events.emit('signUp', '注册成功！用户名、头像可在“我的/个人信息”中修改');
					} else if (operator.from_login) {
						global.events.emit('signIn');
					}
					localStorage.removeItem('curQQLoginState');
				},
				fail(result) {
					Toast.fail(result.message);
				},
				complete: () => {
					this.isLogging = false;
				}
			});
		},
		openQQLogin() {
			let state = Math.floor(Math.random() * 100000);
			localStorage.setItem('curQQLoginState', state.toString());
			let url = 'https://graph.qq.com/oauth2.0/authorize?response_type=code';
			url += '&state=' + state;
			url += '&client_id=' + 101936545;
			if (this.env === 'blog') {
				url += '&redirect_uri=' + 'https://jqh.zone/qq/login';
			} else {
				url += '&redirect_uri=' + 'https://lpt.jqh.zone/qq/login';
			}
			window.location.href = url;
		},
		correctData() {
			const prompt = global.methods.prompt;
			prompt({
				title: '输入校正类型',
				placeholder: '',
				onValidate: () => true,
				onConfirm: (value) => {
					lpt.correctServ.correct({
						param: {
							type: value
						},
						success: (result) => {
							Toast.success(result);
						},
						error: (error) => {
							console.error(error);
						}
					});
				}
			});
		},
		flushData() {
			Dialog.confirm({
				title: '确认',
				message: '是否刷新所有缓存数据到数据库？'
			}).then(() => {
				lpt.correctServ.flush({
					success: (result) => {
						Toast.success(result);
					},
					error: (error) => {
						console.error(error);
					}
				});
			}).catch(() => {
			});
		},
		reloadCategory() {
			Dialog.confirm({
				title: '确认',
				message: '是否从数据库中重载所有目录？'
			}).then(() => {
				lpt.categoryServ.reload({
					success: () => {
						Toast.success('重载成功');
					},
					fail: () => {
						Toast.fail('重载失败');
					}
				});
			}).catch(() => {
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

.ava {
	border-radius: 100%;
	margin: 0.5rem 1rem;
	width: 3.5rem;
	height: 3.5rem;
}

.name {
	margin-top: 1rem;
	font-size: 1.5rem;
}

.cnt-bar-item {
	font-size: 1.15rem;
	font-weight: bold;
	text-align: center;
}

</style>