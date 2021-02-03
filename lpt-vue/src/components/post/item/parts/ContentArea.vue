<template>
	<div class="content-area">
		<p class="title">{{ post.title }}</p>
		<pre class="content" v-if="isShowFullText">{{ fullText }}</pre>
		<pre class="content" v-else>{{ post.content }}</pre>
		<p v-if="post.full_text_id && !isShowFullText" class="full-text-btn" @click="showFullText" >
			查看全文
		</p>
		<p class="category-path">{{ categoryPath }}</p>
	</div>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";

export default {
	name: 'ContentArea',
	props: {
		post: Object
	},
	data() {
		return {
			fullText: '',
			isShowFullText: false
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	computed: {
		categoryPath() {
			return lpt.categoryServ.getPathStr(this.post.category_path);
		}
	},
	methods: {
		showFullText() {
			const ref = this;
			lpt.postServ.getFullText({
				consumer: this.lptConsumer,
				param: {
					fullTextId: this.post.full_text_id
				},
				success(result) {
					ref.isShowFullText = true;
					ref.fullText = result.object;
				}
			});
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
	margin-left: 1rem;
}
</style>