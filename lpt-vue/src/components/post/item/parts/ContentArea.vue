<template>
	<div class="content-area">
		<p class="title">{{ post.title }}</p>
		<pre class="content">{{ post.content }}</pre>
		<p v-if="post.full_text_id" class="full-text-btn" onclick="showFullText()" >查看全文
		</p>
		<p class="category-path">{{ categoryPath }}</p>
	</div>
</template>

<script>
import lpt from "@/lib/js/laputa";

export default {
	name: 'ContentArea',
	props: {
		post: Object
	},
	data() {
		this.categoryPath = lpt.categoryServ.getPathStr(this.post.category_path);
		return {}
	},
	methods: {
		showFullText() {

		}
	}
}
</script>

<style scoped>
.content {
	text-align: left;
	word-break: break-word;
	padding: 0 1rem;
	white-space: pre-wrap;
}

.title {
	text-align: center;
	font-weight: bold;
	word-wrap: break-word;
	word-break: normal;
}

.category-path {
	color: #6c757d;
	text-align: right;
	margin-right: 1rem;
	font-size: 0.75rem;
}

.full-text-btn {
	cursor: pointer;
	color: #007bff;
}
</style>