<template>
	<div class="top-bar">
		<div class="ava-div" style="display: inline-block; float: left">
			<img class="ava" :class="{'def-ava': !comment.creator.raw_avatar}" :src="avatarUrl"
			     @click="showUserHomePage"/>
		</div>
		<div class="time-and-name" style="display: inline-block; float: left">
			<p class="name" @click="showUserHomePage" style="display: inline-block">{{ comment.creator.nick_name }}</p>
			<p v-if="typeof comment.create_time !== 'undefined'" class="time"
				@click="onTimeClick">{{ beforeTime }}</p>
		</div>
		<div v-if="comment.creator.id === posterId" class="poster-tag"
		     style="display: inline-block; float: left; margin-left: 0.5rem">
			<van-tag type="primary">
				贴主
			</van-tag>
		</div>
		<div v-if="comment.is_topped" class="topped-tag" style="display: inline-block; float: left">
			<van-tag type="primary">
				置顶
			</van-tag>
		</div>
	</div>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";
import TimeAgo from "javascript-time-ago";
import zh from 'javascript-time-ago/locale/zh';
import global from "@/lib/js/global";
import moment from 'moment';

TimeAgo.addLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'TopBar',
	props: {
		comment: Object
	},
	data() {
		return {
			showTimeAgo: global.states.showTimeAgo
		}
	},
	computed: {
		posterId() {
			const post = global.states.postManager.get({
				itemId: this.comment.post_id
			});
			return post.creator.id;
		},
		avatarUrl() {
			if (this.comment.creator) {
				return lpt.getUserAvatarUrl(this.comment.creator);
			} else {
				return '';
			}
		},
		beforeTime() {
			if (this.comment.create_time) {
				if (this.showTimeAgo) {
					return timeAgo.format(this.comment.create_time);
				} else {
					return moment(this.comment.create_time).format('YYYY年MM月DD日 HH:mm:ss');
				}
			} else {
				return '';
			}
		}
	},
	methods: {
		showUserHomePage() {
			this.$router.push({
				path: '/user_home_page/' + this.comment.creator.id
			});
		},
		onTimeClick() {
			this.showTimeAgo = !this.showTimeAgo;
		}
	}
}
</script>

<style scoped>
.top-bar {
	height: 2.5rem;
}

.top-bar .time-and-name, .top-bar .topped-tag, .top-bar .poster-tag {
	margin-top: 0;
}

.top-bar .ava {
	border-radius: 100%;
	margin: 0.2rem 1rem;
	width: 2.2rem;
	height: 2.2rem;
}

.top-bar .def-ava {
	margin: 0.075rem 0.75rem;
	width: 2.45rem;
	height: 2.45rem;
}

.top-bar p {
	text-align: left;
	margin: 0;
}

.top-bar .name {
	/* font-weight超过599后，emoji会变成黑白 */
	font-weight: 599;
	font-size: 0.85rem;
	line-height: 0.85rem;
}

.top-bar .time {
	font-size: 0.65rem;
	cursor: pointer;
}

.top-bar .time:hover {
	text-decoration: underline;
}

.topped-tag {
	margin-left: 1rem;
}
</style>