<template>
	<a-row class="top-bar">
		<a-col span="4" class="ava-div">
			<img class="ava" :src="avatarUrl"/>
		</a-col>
		<a-col span="16" class="time-and-name">
			<p class="name">{{ post.creator.nick_name }}</p>
			<p v-if="typeof post.create_time !== 'undefined'" class="time">{{ beforeTime }}</p>
		</a-col>
	</a-row>
</template>

<script>
import lpt from '@/lib/js/laputa'
import TimeAgo from 'javascript-time-ago'
import zh from 'javascript-time-ago/locale/zh'

TimeAgo.addDefaultLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'TopBar',
	props: {
		post: Object
	},
	created() {
	},
	data() {
		this.avatarUrl = lpt.getUserAvatarUrl(this.post.creator);
		this.beforeTime = timeAgo.format(this.post.create_time);
		return {}
	}
}
</script>

<style>
.top-bar {
	height: 3rem;
}

.top-bar .ava {
	border-radius: 100%;
	margin: 0.5rem 1rem;
	width: 2.5rem;
	height: 2.5rem;
}

.top-bar .time-and-name {
	margin-top: 0.52rem;
}

.top-bar p {
	text-align: left;
	margin: 0;
}

.top-bar .name {
	font-weight: bold;
}

.top-bar .time {
	margin-left: 0.15rem;
	font-size: 0.75rem;
}

</style>
