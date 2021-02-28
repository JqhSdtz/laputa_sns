<template>
	<div>
		<p>操作:{{ record.type_str }}</p>
		<p>操作者:</p>
		<user-item :show-id="true" :user="record.creator"/>
		<p>操作对象:</p>
		<post-item v-if="entityType === 'POST'" :post-id="record.desc.id" :show-bottom="false"/>
		<comment-item v-if="entityType === 'CML1'" :comment-id="record.desc.id" type="commentL1" :show-bottom="false"/>
		<comment-item v-if="entityType === 'CML2'" :comment-id="record.desc.id" type="commentL2" :show-bottom="false"/>
		<category-grid-item v-if="entityType === 'CATEGORY'" :category-id="record.desc.id"/>
		<p>操作理由:</p>
		<p style="text-indent: 2rem">{{ record.comment }}</p>
	</div>
</template>

<script>
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';
import UserItem from '@/components/user/item/UserItem';
import CommentItem from '@/components/post/post_detail/comment/item/CommentItem';
import CategoryGridItem from '@/components/category/item/CategoryGridItem';
import {defineAsyncComponent} from 'vue';

export default {
	name: 'AdminOpsRecord',
	components: {
		CategoryGridItem,
		CommentItem,
		PostItem: defineAsyncComponent(() => import('@/components/post/item/PostItem')),
		UserItem
	},
	props: {
		payload: Object
	},
	data() {
		const record = this.payload;
		record.desc = JSON.parse(record.desc);
		const entityType = record.desc.entity_type;
		if (entityType === 'POST') {
			global.states.postManager.add(record.desc);
		} else if (entityType === 'CML1') {
			global.states.commentL1Manager.add(record.desc);
		} else if (entityType === 'CML2') {
			global.states.commentL2Manager.add(record.desc);
		} else if (entityType === 'CATEGORY') {
			global.states.categoryManager.add(record.desc);
		}
		return {
			record,
			entityType,
			commentL1: lpt.commentServ.level1,
			commentL2: lpt.commentServ.level2
		}
	}
}
</script>

<style scoped>

</style>