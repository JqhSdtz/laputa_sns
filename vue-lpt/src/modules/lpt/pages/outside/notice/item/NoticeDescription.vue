<template>
	<div @click="showNoticeDetail">
		<p v-if="notice.type_str === 'like_post'" class="desc">
            帖子<span class="content-desc">{{ description }}</span>收到<span class="unread-cnt">{{ notice.unread_cnt }}</span>条赞
        </p>
        <p v-else-if="notice.type_str === 'like_cml1' || notice.type_str === 'like_cml1'" class="desc">
            评论<span class="content-desc">{{ description }}</span>收到<span class="unread-cnt">{{ notice.unread_cnt }}</span>条赞
        </p>
        <p v-else-if="notice.type_str === 'cml1_of_post'" class="desc">
            帖子<span class="content-desc">{{ description }}</span>收到<span class="unread-cnt">{{ notice.unread_cnt }}</span>条评论
        </p>
        <p v-else-if="notice.type_str === 'cml2_of_cml1'" class="desc">
            评论<span class="content-desc">{{ description }}</span>收到<span class="unread-cnt">{{ notice.unread_cnt }}</span>条回复
        </p>
        <p v-else-if="notice.type_str === 'fw_post'" class="desc">
            帖子<span class="content-desc">{{ description }}</span>收到<span class="unread-cnt">{{ notice.unread_cnt }}</span>条转发
        </p>
        <p v-else-if="notice.type_str === 'follower'" class="desc">
            新增<span class="unread-cnt">{{ notice.unread_cnt }}</span>位粉丝
        </p>
        <p v-else-if="notice.type_str === 'reply_of_cml2'" class="desc">
            帖子<span class="content-desc">{{ description }}</span>收到<span class="unread-cnt">{{ notice.unread_cnt }}</span>条回复
        </p>
	</div>
</template>

<script>

export default {
	name: 'NoticeDescription',
	props: {
		notice: Object
	},
	computed: {
		description() {
			const notice = this.notice;
			let desc = notice.content ? (notice.content.title || notice.content.content) 
						: (notice.type_str + '#' + notice.content_id);
			if (desc.length > 10) {
				desc = desc.substring(0, 9) + '...';
			}
			return desc;
		}
	}
}
</script>

<style scoped>
.desc {
	text-align: center;
	display: inline-block;
	height: 1.5rem;
	line-height: 1.5rem;
}

.content-desc,.unread-cnt {
    font-weight: bold;
}
</style>