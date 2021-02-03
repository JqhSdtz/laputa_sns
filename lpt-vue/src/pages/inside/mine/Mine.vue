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
		<a-row justify="center">
			<a-col class="cnt-bar-item" span="7">
				关注:{{me.user.following_cnt}}
			</a-col>
			<a-col class="cnt-bar-item" span="8">
				粉丝:{{me.user.followers_cnt}}
			</a-col>
			<a-col class="cnt-bar-item" span="7">
				发帖:{{me.user.post_cnt}}
			</a-col>
		</a-row>
		<div style="margin-top: 1.5rem">
			<a-button class="item-btn">
				<router-link to="/home/mod_user_info">
					个人信息
				</router-link>
			</a-button>
			<a-button class="item-btn">
				<router-link to="/home/mod_user_info">
					修改密码
				</router-link>
			</a-button>
			<a-button class="item-btn">
				<router-link to="#" @click="signOut">
					注销
				</router-link>
			</a-button>
		</div>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global/global-state';
import {message} from "ant-design-vue";

export default {
	name: 'Mine',
	data() {
		return {
			me: global.curOperator
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	computed: {
		hasSigned() {
			return global.hasSigned.value;
		},
		myAvatarUrl() {
			return lpt.getUserAvatarUrl(this.me.user);
		}
	},
	methods: {
		signIn() {
			this.$router.push({name: 'signIn'});
		},
		signOut() {
			lpt.operatorServ.signOut({
				consumer: this.lptConsumer,
				success() {
					message.success('注销成功');
				},
				fail(result) {
					message.error(result.message);
				}
			});
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

.item-btn {
	width: 100%;
	height: 3rem;
	text-align: left;
	text-indent: 1rem;
	border-bottom: none;
}

.cnt-bar-item {
	font-size: 1.15rem;
	font-weight: bold;
	text-align: center;
}

</style>