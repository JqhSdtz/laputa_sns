<template>
	<div class="top-bar">
		<div class="ava-div" style="display: inline-block; float: left">
			<img class="ava" :class="{'def-ava': !post.creator.raw_avatar}" :src="avatarUrl" @click="showUserHomePage"/>
		</div>
		<div class="time-and-name" style="display: inline-block; float: left">
			<p class="name" @click="showUserHomePage">{{ post.creator.nick_name }}</p>
			<p v-if="typeof post.create_time !== 'undefined'" class="time"
				@click="onTimeClick">{{ beforeTime }}</p>
		</div>
		<div v-if="isTopPost" class="topped-tag" style="display: inline-block; float: left">
			<van-tag  type="primary">
				置顶
			</van-tag>
		</div>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import TimeAgo from 'javascript-time-ago';
import zh from 'javascript-time-ago/locale/zh';
import global from "@/lib/js/global";
import moment from 'moment';

TimeAgo.addLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'TopBar',
	props: {
		postId: Number,
		isTopPost: Boolean
	},
	data() {
		const post = global.states.postManager.get({
			itemId: this.postId
		});
		return {
			post: post,
			showTimeAgo: global.states.showTimeAgo
		}
	},
	computed: {
		avatarUrl() {
			if (this.post.creator) {
				return lpt.getUserAvatarUrl(this.post.creator);
			} else {
				return '';
			}
		},
		beforeTime() {
			if (this.post.create_time) {
				if (this.showTimeAgo) {
					return timeAgo.format(this.post.create_time);
				} else {
					return moment(this.post.create_time).format('YYYY年MM月DD日 HH:mm:ss');
				}
			} else {
				return '';
			}
		}
	},
	methods: {
		showUserHomePage() {
			this.$router.push({
				path: '/user_home_page/' + this.post.creator.id
			});
		},
		onTimeClick() {
			this.showTimeAgo = !this.showTimeAgo;
		}
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

.top-bar .def-ava {
	margin: 0.375rem 0.75rem;
	width: 2.75rem;
	height: 2.75rem;
}

.top-bar .time-and-name, .top-bar .topped-tag {
	margin-top: 0.42rem;
}

.top-bar p {
	text-align: left;
	margin: 0;
}

.top-bar .name {
	/* font-weight超过599后，emoji会变成黑白 */
	font-weight: 599;
}

.top-bar .time {
	font-size: 0.75rem;
	cursor: pointer;
}

.top-bar .time:hover {
	text-decoration: underline;
}

.topped-tag {
	margin-left: 1rem;
}

</style>
