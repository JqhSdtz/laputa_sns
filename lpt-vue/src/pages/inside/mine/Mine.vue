<template>
	<div class="mine">
		<a-row style="padding-top: 2rem;">
			<a-col span="5" offset="2">
				<img class="ava" :src="myAvatarUrl"/>
			</a-col>
			<a-col span="16" offset="1">
				<p v-if="hasSigned" class="name">{{ me.user.nick_name }}</p>
				<a-button v-else type="link" class="name" @click="signIn">点击登录</a-button>
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
					<a-badge style="margin-left: 1rem; margin-top: -0.5rem;" :count="me.unread_notice_cnt" />
				</template>
			</van-cell>
			<van-cell title="个人信息" is-link to="/mod_user_info"/>
			<van-cell title="修改密码" is-link to="/mod_user_info"/>
			<van-cell title="注销" is-link @click="signOut"/>
		</div>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {Dialog, Toast} from 'vant';

export default {
	name: 'Mine',
	data() {
		return {
			me: global.states.curOperator
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		if (global.states.hasSigned.value) {
			lpt.userServ.get({
				consumer: this.lptConsumer,
				param: {
					userId: this.me.user.id
				},
				success(result) {
					global.states.curOperator.user = result.object;
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		}
	},
	computed: {
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
				name: 'followersList',
				params: {
					userId: this.me.user.id.toString()
				}
			});
		},
		showFollowingList() {
			this.$router.push({
				name: 'followingList',
				params: {
					userId: this.me.user.id.toString()
				}
			});
		},
		signIn() {
			this.$router.push({name: 'signIn'});
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
				});
			}).catch(() => {});
		}
	}
}
</script>

<style scoped>
.mine {
	height: 100%;
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