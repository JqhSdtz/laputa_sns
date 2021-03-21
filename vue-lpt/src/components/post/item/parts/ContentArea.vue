<template>
	<div class="content-area">
		<p class="title" @click="showPostDetail">{{ post.title }}</p>
		<pre class="content" v-if="isShowFullText" @click="showPostDetail">
			<p v-if="postType === 'normal'">{{ fullText }}</p>
			<admin-ops-record v-if="postType === 'amOps' && payload" :payload="payload"/>
			<v-md-preview v-if="postType === 'md'" :text="fullText"></v-md-preview>
		</pre>
		<pre class="content" v-else @click="showPostDetail">{{ postContent }}</pre>
		<p v-if="post.full_text_id && !isShowFullText" class="full-text-btn" @click.stop="showFullText">
			查看全文
		</p>
		<p v-if="isShowFullText" class="full-text-btn" @click.stop="hideFullText">
			收起全文
		</p>
		<div v-if="imgList.length > 0">
			<a v-for="(img, idx) in imgList" :key="img" @click="showImgPreview(idx)"
			   style="display: inline; margin-left: 1em">
				<van-image :src="img" width="50" height="50"/>
			</a>
		</div>
		<a-row tyle="flex" justify="end">
			<a-col pull="1">
				<p v-if="post.editable" class="editable-label">可被修改</p>
			</a-col>
			<a-col>
				<category-path v-if="post.type_str === 'public'" :path-list="post.category_path" class="category-path"/>
			</a-col>
		</a-row>
		<slot/>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import CategoryPath from '@/components/category/CategoryPath';
import {ImagePreview} from 'vant';
import AdminOpsRecord from '@/components/post/item/parts/AdminOpsRecord';

const typeReg = /tp:([a-zA-Z]*)#/;

export default {
	name: 'ContentArea',
	props: {
		post: Object
	},
	components: {
		AdminOpsRecord,
		CategoryPath
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
		let postContent = this.post.content;
		let postType = 'normal';
		if (typeReg.test(this.post.content)) {
			postType = this.post.content.match(typeReg)[1];
			postContent = this.post.content.replace(typeReg, '');
		}
		return {
			fullUrlList: fullUrlList,
			imgList: imgList,
			postContent,
			payload: '',
			postType,
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
		processAmOps(oriText) {
			try {
				this.payload = JSON.parse(oriText);
			} catch (e) {
				console.error(e);
				return '';
			}
		},
		showFullText() {
			const ref = this;
			if (this.fullText) {
				this.isShowFullText = true;
			} else {
				lpt.postServ.getFullText({
					consumer: this.lptConsumer,
					param: {
						fullTextId: this.post.full_text_id
					},
					success(result) {
						ref.isShowFullText = true;
						if (ref.postType === 'amOps') {
							ref.processAmOps(result.object);
						} else {
							ref.fullText = result.object;
						}
					}
				});
			}
		},
		hideFullText() {
			this.isShowFullText = false;
		}
	}
}
</script>

<style scoped>
.content {
	text-align: left;
	word-break: break-all;
	padding: 0 1rem;
	white-space: pre-wrap;
}

.title {
	text-align: center;
	font-weight: bold;
	word-wrap: break-word;
	word-break: normal;
}

.editable-label {
	color: #6c757d;
	margin-left: 1rem;
	font-size: 0.75rem;
}

.category-path {
	color: #6c757d;
	margin-right: 1rem;
	font-size: 0.75rem;
}

.full-text-btn {
	cursor: pointer;
	color: #007bff;
	margin-left: 1rem;
}
</style>