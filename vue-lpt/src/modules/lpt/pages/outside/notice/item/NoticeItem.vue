<template>
	<div style="margin-left: 2rem;" @click="showNoticeDetail">
		<div style="height: 1.5rem; margin-top: 1rem;">
			<a-badge v-if="unread" :dot="true" :offset="[6, 0]">
				<notice-description class="notice-desc" :notice="notice"/> 
			</a-badge>
			<notice-description v-else class="notice-desc" :notice="notice"/> 
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
import NoticeDescription from './NoticeDescription';

TimeAgo.addLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'NoticeItem',
	components: {
		NoticeDescription
	},
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
			if (typeIs('like_post') || typeIs('fw_post') || typeIs('cml1_of_post')) {
				path = '/post_detail/' + this.notice.content_id;
				if (typeIs('like_post')) {
					query.command = 'showLikeList';
				} else if(typeIs('fw_post')) {
					query.command = 'showForwardList';
				}
			}  else if (typeIs('cml2_of_cml1') || typeIs('reply_of_cml2')) {
				path = '/post_detail/' + this.notice.content.post_id;
				query.command = 'showCommentDetail';
				if (typeIs('cml2_of_cml1')) {
					query.commentId = this.notice.content_id;
				} else if (typeIs('reply_of_cml2')) {
					query.commentId = this.notice.content.l1_id;
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

.notice-desc {
	display: inline-block;
}
</style>