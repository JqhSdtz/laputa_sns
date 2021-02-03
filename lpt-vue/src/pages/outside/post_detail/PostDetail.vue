<template>
	<div style="margin: 1rem 0.5rem 0 0.5rem;">
		<div class="post-item">
			<top-bar class="top-bar" :post="post"></top-bar>
			<content-area class="content-area" :post="post"></content-area>
		</div>
	</div>
	<div id="middle-bar" style="width: 100%">
		<a-tabs v-model:activeKey="curTabKey">
			<a-tab-pane key="forward" :tab="'转发 ' + post.forward_cnt">
				Content of Tab Pane 1
			</a-tab-pane>
			<a-tab-pane key="comment" :tab="'评论 ' + post.comment_cnt">
				Content of Tab Pane 2
			</a-tab-pane>
			<a-tab-pane key="like" :tab="'赞 ' + post.like_cnt">
				Content of Tab Pane 3
			</a-tab-pane>
		</a-tabs>
	</div>
	<bottom-bar style="position: fixed;bottom: 0" :post_id="post.id"/>
</template>

<script>
import TopBar from '@/components/post/item/parts/TopBar';
import ContentArea from '@/components/post/item/parts/ContentArea';
import BottomBar from './BottomBar';
import {message} from "ant-design-vue";
import lpt from '@/lib/js/laputa/laputa';
import global from "@/lib/js/global/global-state";

export default {
	name: 'PostDetail',
	props: {
		post_id: String
	},
	components: {
		TopBar,
		ContentArea,
		BottomBar
	},
	data() {
		return {
			post: global.postManager.get(this.post_id),
			curTabKey: 'comment'
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		const ref = this;
		lpt.postServ.get({
			consumer: this.lptConsumer,
			param: {
				post_id: this.post_id
			},
			success(result) {
				ref.post = result.object;
			},
			fail(result) {
				message.error(result.message);
			}
		})
		this.commentQueriorMap = new Map();
	}
}
</script>

<style scoped>
:global(.ant-tabs-tab) {
	width: 20% !important;
	margin: 0 6.66% !important;
	text-align: center !important;
}
:global(.ant-tabs-nav) {
	width: 100% !important;
}
</style>