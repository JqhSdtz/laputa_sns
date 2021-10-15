<template>
	<div style="margin-left: 2rem;" @click="showNoticeDetail">
		<div style="height: 1.5rem; margin-top: 1rem;">
			<a-badge v-if="unread" :dot="true" :offset="[6, 0]">
				<p class="desc">{{getDescription()}}</p>
			</a-badge>
			<p v-else class="desc">{{getDescription()}}</p>
			<p class="time" style="margin-left: 1rem">{{updateTime}}</p>
		</div>
		<van-divider style="margin: 5px 0"/>
	</div>
</template>

<script>
import TimeAgo from 'javascript-time-ago';
import zh from 'javascript-time-ago/locale/zh';
import {Toast} from 'vant';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';

TimeAgo.addLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'NoticeItem',
	props: {
		noticeId: String
	},
	data() {
		const notice = global.states.noticeManager.get({
			itemId: this.noticeId
		});
		return {
			notice
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	computed: {
		updateTime() {
			if (this.notice.update_time) {
				return timeAgo.format(this.notice.update_time);
			} else {
				return '';
			}
		},
		unread() {
			return this.notice.init_time !== 0 && !this.notice.hasRead;
		}
	},
	methods: {
		getDescription() {
			const notice = this.notice;
			const typeStr = notice.type_str;
			const title = (typeof notice.content === 'undefined' || notice.content == null) ? null : notice.content.title;
			let desc = '';
			if (typeof title !== 'undefined' && title !== null) {
				desc = title;
			} else if (notice.content != null) {
				desc = notice.content.content;
			}
			if (desc.length > 10) {
				desc = desc.substring(0, 9) + '...';
			}
			switch (typeStr) {
				case 'like_post':
					return '帖子"' + desc + '"收到' + notice.unread_cnt + '条赞';
				case 'like_cml1':
				case 'like_cml2':
					return '评论"' + desc + '"收到' + notice.unread_cnt + '条赞';
				case 'cml1_of_post':
					return '帖子"' + desc + '"收到' + notice.unread_cnt + '条评论';
				case 'cml2_of_cml1':
					return '评论"' + desc + '"收到' + notice.unread_cnt + '条回复';
				case 'fw_post':
					return '帖子"' + desc + '"收到' + notice.unread_cnt + '条转发';
				case 'follower':
					return '新增' + notice.unread_cnt + '位粉丝';
				case 'reply_of_cml2':
					return '评论"' + desc + '"收到' + notice.unread_cnt + '条回复';
			}
		},
		showNoticeDetail() {
			const ref = this;
			lpt.noticeServ.markAsRead({
				consumer: this.lptConsumer,
				data: {
					type: this.notice.type,
					content_id: this.notice.content_id
				},
				success() {
					ref.notice.hasRead = true;
					--global.states.curOperator.unread_notice_cnt;
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
			if (this.notice.content && this.notice.content.deleted) {
				Toast.fail('该内容已被删除');
				return;
			}
			let path = '';
			const query = {};
			const typeStr = this.notice.type_str;
			const typeIs = (str) => typeStr === str;
			if (typeIs('like_post') || typeIs('fw_post') || typeIs('cml2_of_cml1')
					|| typeIs('cml1_of_post')) {
				path = '/post_detail/' + this.notice.content_id;
				if (typeIs('like_post')) {
					query.command = 'showLikeList';
				} else if(typeIs('fw_post')) {
					query.command = 'showForwardList';
				} else if (typeIs('cml2_of_cml1')) {
					query.command = 'showCommentDetail';
					query.commentId = this.notice.content.post_id;
				}
			} else if (typeIs('like_cml1') || typeIs('like_cml2')) {
				const type = typeIs('like_cml1') ? lpt.commentServ.level1 : lpt.commentServ.level2;
				path = `/comment_like_list/${type}/${this.notice.content_id}`;
			} else if (typeIs('follower')) {
				path = '/followers_list/' + global.states.curOperator.user.id;
			}
			this.$router.push({
				path: path,
				query: query
			});
		}
	}
}
</script>

<style scoped>
p {
	text-align: center;
	display: inline-block;
	height: 1.5rem;
	line-height: 1.5rem;
}
</style>