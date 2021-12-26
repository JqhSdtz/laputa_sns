<template>
	<a-row class="forward-item">
		<a-col span="4" class="ava-div">
			<img class="ava" :src="avatarUrl"/>
		</a-col>
		<a-col span="16" class="time-and-name">
			<p class="name">{{ forward.creator.nick_name }}</p>
			<p v-if="typeof forward.create_time !== 'undefined'" class="time">{{ beforeTime }}</p>
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
	name: 'ForwardItem',
	props: {
		forward: Object
	},
	computed: {
		avatarUrl() {
			if (this.forward.creator) {
				return lpt.getUserAvatarUrl(this.forward.creator);
			} else {
				return '';
			}
		},
		beforeTime() {
			if (this.forward.create_time) {
				return timeAgo.format(this.forward.create_time);
			} else {
				return '';
			}
		}
	}
}
</script>

<style scoped>
.forward-item {
	background-color: white;
	/*height: 2.5rem;*/
}

.forward-item .time-and-name {
	margin-top: 0.3rem;
}

.forward-item .ava {
	border-radius: 100%;
	margin: 0.2rem 1rem;
	width: 2.2rem;
	height: 2.2rem;
}

.forward-item p {
	text-align: left;
	margin: 0;
}

.forward-item .name {
	/* font-weight超过599后，emoji会变成黑白 */
	font-weight: 599;
	font-size: 0.85rem;
}

.forward-item .time {
	margin-left: 0.15rem;
	font-size: 0.65rem;
}
</style>