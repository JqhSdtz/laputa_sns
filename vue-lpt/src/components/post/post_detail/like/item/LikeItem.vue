<template>
	<a-row class="like-item">
		<a-col span="4" class="ava-div">
			<img class="ava" :src="avatarUrl"/>
		</a-col>
		<a-col span="16" class="time-and-name">
			<p class="name">{{ like.creator.nick_name }}</p>
			<p v-if="typeof like.create_time !== 'undefined'" class="time">{{ beforeTime }}</p>
		</a-col>
		<van-divider style="margin: 5px 0"/>
	</a-row>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";
import TimeAgo from "javascript-time-ago";
import zh from 'javascript-time-ago/locale/zh';

TimeAgo.addLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'LikeItem',
	props: {
		like: Object
	},
	computed: {
		avatarUrl() {
			if (this.like.creator) {
				return lpt.getUserAvatarUrl(this.like.creator);
			} else {
				return '';
			}
		},
		beforeTime() {
			if (this.like.create_time) {
				return timeAgo.format(this.like.create_time);
			} else {
				return '';
			}
		}
	}
}
</script>

<style scoped>
.like-item {
	background-color: white;
	/*height: 2.5rem;*/
}

.like-item .time-and-name {
	margin-top: 0.3rem;
}

.like-item .ava {
	border-radius: 100%;
	margin: 0.2rem 1rem;
	width: 2.2rem;
	height: 2.2rem;
}

.like-item p {
	text-align: left;
	margin: 0;
}

.like-item .name {
	/* font-weight超过599后，emoji会变成黑白 */
	font-weight: 599;
	font-size: 0.85rem;
}

.like-item .time {
	margin-left: 0.15rem;
	font-size: 0.65rem;
}
</style>