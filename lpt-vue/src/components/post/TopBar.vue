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
	height: 3rem;
	position: relative;
	border-bottom: 1px solid rgba(0, 0, 0, .125);
}


.top-bar .ava-div {
	position: absolute;
}

.top-bar .ava {
	border-radius: 100%;
	width: 3rem;
	height: 3rem;
}

.top-bar .time-and-name {
	position: absolute;
	transform: translate(0, -50%);
	top: 50%;
	left: 4.75rem;
}

.top-bar p {
	text-align: left;
}

.top-bar .name {
	font-weight: bold;
	font-size: 1.1rem;
}

.top-bar .time {
	margin-top: -1rem;
	text-indent: 0.25rem;
	font-size: 0.85rem;
}

</style>
