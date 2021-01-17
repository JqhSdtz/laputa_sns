<template>
	<div class="top-bar">
		<div class="ava-div"><img class="ava" :src="avatarUrl"/></div>
		<div class="time-and-name">
			<p class="name">{{ post.creator.nick_name }}</p>
			<p v-if="typeof post.create_time !== 'undefined'" class="time">{{ beforeTime }}</p>
		</div>
	</div>
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
	background-color: rgb(247, 247, 247);
	padding: 0.5rem 1rem;
	height: 3.5rem;
	position: relative;
	border-bottom: 1px solid rgba(0, 0, 0, .125);
}


.top-bar .ava-div {
	position: absolute;
}

.top-bar .ava {
	border-radius: 100%;
	width: 3.5rem;
	height: 3.5rem;
}

.top-bar .time-and-name {
	position: absolute;
	margin-left: 4.5rem;
}

.top-bar .name {
	margin-top: 0.4rem;
	margin-bottom: 0.5rem;
	font-weight: bold;
	font-size: 1.25rem;
}

.top-bar .time {
	margin-top: 0;
	font-size: 1rem;
}

</style>
