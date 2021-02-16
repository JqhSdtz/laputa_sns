<template>
	<a-row class="user-item" @click="showUserHomePage">
		<a-col span="4" class="ava-div">
			<img class="ava" :src="avatarUrl"/>
		</a-col>
		<a-col class="name">
			<p>{{user.nick_name}}</p>
		</a-col>
		<van-divider style="margin: 5px 0"/>
	</a-row>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";

export default {
	name: 'UserItem',
	props: {
		user: Object
	},
	computed: {
		avatarUrl() {
			if (this.user) {
				return lpt.getUserAvatarUrl(this.user);
			} else {
				return '';
			}
		}
	},
	methods: {
		showUserHomePage() {
			this.$router.push({
				name: 'userHomePage',
				params: {
					userId: this.user.id
				}
			});
		}
	}
}
</script>

<style scoped>
.user-item {
	padding: 1rem 10%;
}

.user-item .ava {
	border-radius: 100%;
	margin: 0.2rem 1rem;
	width: 2.2rem;
	height: 2.2rem;
}

.user-item .name {
	text-align: left;
	margin-left: 1.5rem;
	font-size: 1.15rem;
	height: 2.4rem;
	line-height: 2.4rem;
}
</style>