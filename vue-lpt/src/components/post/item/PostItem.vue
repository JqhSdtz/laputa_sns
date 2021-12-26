<template>
	<div class="post-item">
		<top-bar class="top-bar" :post-id="post.id" :is-top-post="isTopPost"/>
		<content-area class="content-area" :post-id="post.id" :show-full-text="showFullText">
			<div v-if="post.ori_post" style="box-shadow: 0 -1.5px 10px rgba(100, 100, 100, 0.2);">
				<post-item :show-bottom="false" :post-id="post.ori_post.id"/>
			</div>
		</content-area>
		<a-row tyle="flex" justify="end">
			<a-col pull="1">
				<p v-if="post.editable" class="editable-label text-link"
					@click="showEditablePrompt">
					可被修改
				</p>
			</a-col>
			<a-col>
				<category-path v-if="post.type_str === 'public'" :path-list="post.category_path" class="category-path"/>
			</a-col>
		</a-row>
		<bottom-bar v-if="showBottom" class="bottom-bar" :post-id="post.id" :post-of="postOf" :show-actions="true"/>
	</div>
</template>

<script>
import TopBar from './parts/TopBar.vue';
import ContentArea from './parts/ContentArea';
import BottomBar from './parts/BottomBar';
import CategoryPath from '@/components/category/CategoryPath';
import global from '@/lib/js/global';

export default {
	name: 'PostItem',
	props: {
		showBottom: {
			type: Boolean,
			default: true
		},
		showActionsOnContentArea: Boolean,
		showFullText: Boolean,
		postId: Number,
		postOf: String,
		isTopPost: Boolean
	},
	inject: {
		prompts: {
			type: Object
		}
	},
	data() {
		return {
			post: global.states.postManager.get({
				itemId: this.postId
			})
		};
	},
	components: {
		TopBar,
		ContentArea,
		BottomBar,
		CategoryPath
	},
	methods: {
		showEditablePrompt() {
			this.prompts.alert({
				title: '提示',
				message: '该帖可能已经过多次修改，并在将来仍可能被修改'
			});
		}
	}
}
</script>

<style scoped>
.post-item {
	background-color: white;
	margin-bottom: 1rem;
}

.content-area {
	margin-top: 0.5rem;
}

.bottom-bar {
	z-index: 2;
}

.editable-label {
	color: #6c757d;
	margin-left: 1rem;
	font-size: 0.85rem;
}

.category-path {
	color: #6c757d;
	margin-right: 1rem;
	font-size: 0.85rem;
}

.text-link {
	cursor: pointer;
}

.text-link:hover {
	text-decoration: underline;
}
</style>