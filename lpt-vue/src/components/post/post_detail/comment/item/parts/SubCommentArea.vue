<template>
	<div style="width: 100%; background-color: #ECECEC">
		<div v-for="comment in comment.preview_l2_list" :key="comment.id">
			<a-button type="link">
				{{comment.creator.nick_name}}
			</a-button>
			<van-tag v-if="comment.creator.id === curUserId" type="primary" style="margin-left: -0.75rem">
				贴主
			</van-tag>
			<div style="display: inline-block; margin-left: 0.25rem">:</div>
			<div style="display: inline-block; margin-left: 0.5rem" @click="openCommentDetail">
				{{comment.content}}
			</div>
		</div>
		<a-button type="link" v-if="previewL2Length < comment.l2_cnt" @click="openCommentDetail">
			共{{comment.l2_cnt}}条回复
			<span v-if="comment.poster_rep_cnt">
				含贴主{{comment.poster_rep_cnt}}条
			</span>
		</a-button>
	</div>
</template>

<script>
import global from '@/lib/js/global';

export default {
	name: 'SubCommentArea',
	props: {
		comment: Object
	},
	inject: {
		localEvents: {
			type: Object
		}
	},
	computed: {
		curUserId() {
			return global.states.curOperator.user.id;
		},
		previewL2Length() {
			const list = this.comment.preview_l2_list;
			return list ? list.length : 0;
		}
	},
	methods: {
		openCommentDetail() {
			this.localEvents.emit('openCommentDetail', {
				id: this.comment.id
			});
		}
	}
}
</script>

<style scoped>

</style>