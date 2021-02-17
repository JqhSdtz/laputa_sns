<template>
	<a-row class="user-item" @click="showUserHomePage">
		<a-col span="4" class="ava-div">
			<img class="ava" :class="{'def-ava': !user.raw_avatar}" :src="avatarUrl"/>
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
	margin: 0.5rem 1rem;
	width: 2.5rem;
	height: 2.5rem;
}

.user-item .def-ava {
	margin: 0.375rem 0.75rem;
	width: 2.75rem;
	height: 2.75rem;
}

.user-item .name {
	text-align: left;
	margin-left: 1.5rem;
	font-size: 1.15rem;
	height: 3.1rem;
	line-height: 3.1rem;
}
</style>