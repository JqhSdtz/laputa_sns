<template>
	<div class="content-area">
		<p class="title">{{ post.title }}</p>
		<pre class="content" v-if="isShowFullText" @click="showPostDetail">{{ fullText }}</pre>
		<pre class="content" v-else @click="showPostDetail">{{ post.content }}</pre>
		<p v-if="post.full_text_id && !isShowFullText" class="full-text-btn" @click="showFullText">
			查看全文
		</p>
		<div v-if="imgList.length > 0">
			<a v-for="(img, idx) in imgList" :key="img" @click="showImgPreview(idx)" style="display: inline; margin-left: 1em">
				<van-image :src="img" width="50" height="50" />
			</a>
		</div>
		<p v-if="post.type_str === 'public'" class="category-path">{{ categoryPath }}</p>
	</div>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";
import { ImagePreview } from 'vant';

export default {
	name: 'ContentArea',
	props: {
		post: Object
	},
	data() {
		const rawImg = this.post.raw_img || '';
		const imgListStr = rawImg.split('#');
		const imgList = [];
		const fullUrlList = [];
		imgListStr.forEach(img => {
			if (img) {
				imgList.push(lpt.getPostThumbUrl(img));
				fullUrlList.push(lpt.getFullImgUrl(img));
			}
		});
		return {
			fullUrlList: fullUrlList,
			imgList: imgList,
			fullText: '',
			isShowFullText: false
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	computed: {
		categoryPath() {
			if (!this.post.category_path) {
				return '';
			}
			return lpt.categoryServ.getPathStr(this.post.category_path);
		}
	},
	methods: {
		showPostDetail() {
			this.$router.push({
				name: 'postDetail',
				params: {
					postId: this.post.id
				}
			});
		},
		showImgPreview(index) {
			ImagePreview({
				images: this.fullUrlList,
				startPosition: index
			});
		},
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