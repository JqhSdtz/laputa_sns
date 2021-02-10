<template>
	<a-row class="top-bar">
		<a-col span="4" class="ava-div">
			<img class="ava" :src="avatarUrl"/>
		</a-col>
		<a-col span="16" class="time-and-name">
			<p class="name">{{ comment.creator.nick_name }}</p>
			<p v-if="typeof comment.create_time !== 'undefined'" class="time">{{ beforeTime }}</p>
		</a-col>
	</a-row>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";
import TimeAgo from "javascript-time-ago";
import zh from 'javascript-time-ago/locale/zh';

TimeAgo.addLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'TopBar',
	props: {
		comment: Object
	},
	computed: {
		avatarUrl() {
			if (this.comment.creator) {
				return lpt.getUserAvatarUrl(this.comment.creator);
			} else {
				return '';
			}
		},
		beforeTime() {
			if (this.comment.create_time) {
				return timeAgo.format(this.comment.create_time);
			} else {
				return '';
			}
		}
	}
}
</script>

<style scoped>
.top-bar {
	height: 2.5rem;
}

.top-bar .time-and-name {
	margin-top: 0.3rem;
}

.top-bar .ava {
	border-radius: 100%;
	margin: 0.2rem 1rem;
	width: 2.2rem;
	height: 2.2rem;
}

.top-bar p {
	text-align: left;
	margin: 0;
}

.top-bar .name {
	font-weight: bold;
	font-size: 0.85rem;
}

.top-bar .time {
	margin-left: 0.15rem;
	font-size: 0.65rem;
}
</style>