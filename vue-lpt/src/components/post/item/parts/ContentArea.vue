<template>
	<div class="content-area">
		<p class="title" @click="showPostDetail">{{ post.title }}</p>
		<div class="content" v-if="isShowFullText" @click="showPostDetail">
			<p v-if="postType === 'normal'" style="margin-bottom: 0;">{{ fullText }}</p>
			<admin-ops-record v-if="postType === 'amOps' && payload" :payload="payload"/>
			<v-md-preview v-if="postType === 'md'" :text="fullText"/>
		</div>
		<div class="content" v-if="!isShowFullText" @click="showPostDetail">
			<v-md-preview v-if="postType === 'md' && !post.full_text_id" :text="postContent"/>
			<ellipsis v-else :content="postContent" :rows="5"/>
		</div>
		<p v-if="post.full_text_id && !post.noFullText && !isShowFullText" class="full-text-btn" @click.stop="showFullText">
			查看全文
		</p>
		<p v-if="isShowFullText" class="full-text-btn" @click.stop="hideFullText">
			收起全文
		</p>
		<div v-if="imgList.length > 0">
			<div v-if="env === 'lpt'">
				<a v-for="(img, idx) in imgList" :key="img" @click="showImgPreview(idx)"
				   style="display: inline; margin-left: 1rem">
					<van-image :src="img" width="50" height="50"/>
				</a>
			</div>
			<div v-if="env === 'blog'" class="image-box-list">
				<image-box :images="imageBoxList"/>
			</div>
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
import global from '@/lib/js/global';
import CategoryPath from '@/components/category/CategoryPath';
import {ImagePreview} from 'vant';
import AdminOpsRecord from '@/components/post/item/parts/AdminOpsRecord';
import ImageBox from '@/components/global/ImageBox';
import Ellipsis from '@/components/global/Ellipsis';

const typeReg = /tp:([a-zA-Z]*)#/;

export default {
	name: 'ContentArea',
	props: {
		post: Object
	},
	components: {
		AdminOpsRecord,
		CategoryPath,
		ImageBox,
		Ellipsis
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	data() {
		return {
			env: global.vars.env,
			fullUrlList: [],
			imgList: [],
			imageBoxList: [],
			postContent: this.post.customContent || this.post.content,
			payload: '',
			postType: 'normal',
			fullText: '',
			isShowFullText: false
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	watch: {
		'post.content': {
			immediate: true,
			handler() {
				if (typeReg.test(this.post.content)) {
					this.postType = this.post.content.match(typeReg)[1];
					const content = this.post.customContent || this.post.content;
					this.postContent = content.replace(typeReg, '');
				}
			}
		},
		'post.raw_img': {
			immediate: true,
			handler() {
				const rawImg = this.post.raw_img || '';
				const imgListStr = rawImg.split('#');
				imgListStr.forEach(img => {
					if (!img) return;
					const fullImgUrl = lpt.getFullImgUrl(img, Math.floor(this.clientWidth));
					this.imgList.push(lpt.getPostThumbUrl(img));
					this.fullUrlList.push(fullImgUrl);
					this.imageBoxList.push({
						thumb: lpt.getPostThumbUrl(img),
						src: fullImgUrl
					});
				});
			}
		}
	},
	computed: {
		categoryPath() {
			if (!this.post.category_path) {
				return '';
			}
			return lpt.categoryServ.getPathStr(this.post.category_path);
		},
		clientWidth() {
			// 巨坑，深扒van-tabs组件发现是在swipe组件中获取了一个tabs的宽度
			// 但是如果tabs组件不占满屏幕，又没有固定的px值，则返回0
			// tabs组件错乱
			if (global.vars.env === 'blog' && this.lptContainer === 'blogDrawer') {
				return global.states.style.drawerWidth;
			} else {
				return global.states.style.bodyWidth;
			}
		}
	},
	methods: {
		getContainer() {
			return window.document.body;
		},
		showPostDetail() {
			if (this.lptContainer === 'blogMain') {
				this.$router.push({
					path: '/blog/post_detail/' + this.post.id
				});
			} else {
				this.$router.push({
					path: '/post_detail/' + this.post.id
				});
			}
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
			if (this.fullText) {
				this.isShowFullText = true;
			} else if (this.post.customFullText) {
				this.fullText = this.post.customFullText;
				this.isShowFullText = true;
			} else if(this.post.full_text) {
				this.fullText = this.post.full_text;
				this.isShowFullText = true;
			} else {
				lpt.postServ.getFullText({
					consumer: this.lptConsumer,
					param: {
						fullTextId: this.post.full_text_id
					},
					success: (result) => {
						this.isShowFullText = true;
						global.states.postManager.add({
							...this.post,
							full_text: result.object
						});
						if (this.postType === 'amOps') {
							this.processAmOps(result.object);
						} else {
							this.fullText = result.object;
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
	font-size: 0.9rem;
	white-space: pre-wrap;
	margin-bottom: 0.5rem;
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
	font-size: 0.85rem;
}

.category-path {
	color: #6c757d;
	margin-right: 1rem;
	font-size: 0.85rem;
}

.full-text-btn {
	cursor: pointer;
	color: #007bff;
	font-size: 0.9rem;
	margin-left: 1rem;
}

:global(.image-box-list img) {
	display: inline;
	margin-left: 1rem;
}

:global(.image-box-list>div>div) {
	display: inline-block;
}

:global(.vue-lb-info) {
	/*bottom: -20px !important;*/
	display: none;
}
</style>