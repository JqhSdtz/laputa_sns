<template>
	<div class="content-area">
		<p class="title" @click="showPostDetail">{{ post.title }}</p>
		<!-- gallery-item放到外面是为了防止img元素闪烁 -->
		<gallery-item-content v-if="contentType === 'gallery'" :gallery-item="galleryItem"/>
		<div v-if="isShowFullText" class="content full-text" 
			:class="{'md-content': fullTextType === 'md'}">
			<p ref="normalFullText" 
				v-if="fullTextType === 'normal'" 
				style="margin-bottom: 0;"
				@click="showPostDetail">
				{{ fullText }}
			</p>
			<admin-ops-record v-if="fullTextType === 'amOps' && payload" :payload="payload"/>
			<v-md-preview ref="fullTextMd" 
				v-if="fullTextType === 'md'" 
				:text="fullText"
				@click="showPostDetail"/>
			<!-- 解析gallery-item时会设置customContent -->
			<p v-if="fullTextType === 'gallery' && post.customFullText" 
				style="margin: 1rem 0 0 0;"
				@click="showPostDetail">
				{{ post.customFullText }}
			</p>
		</div>
		<div v-if="!isShowFullText" class="content abstract" 
			:class="{'md-content': contentType === 'md'}">
			<v-md-preview ref="contentMd" 
				v-if="contentType === 'md'" 
				:text="postContent"
				@click="showPostDetail"/>
			<ellipsis v-if="contentType === 'normal' || contentType === 'amOps'" 
				:content="postContent" 
				:rows="5"
				@click="showPostDetail"/>
		</div>
		<p v-if="hasFullText && !isShowFullText" class="full-text-btn" @click.stop="showFullTextFun">
			查看全文
		</p>
		<p v-if="hasFullText && isShowFullText" class="full-text-btn" @click.stop="hideFullText">
			收起全文
		</p>
		<post-image-list v-if="imgUrlList.length > 0" style="margin-bottom: 1rem"
		                 :img-url-list="imgUrlList"
		                 :full-url-list="fullUrlList"
		                 :image-box-list="imageBoxList"/>
		<slot/>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {translateMd} from '@/lib/js/markdown/md-translator';
import AdminOpsRecord from './AdminOpsRecord';
import GalleryItemContent from './GalleryItemContent';
import PostImageList from './PostImageList';
import Ellipsis from '@/components/global/Ellipsis';

const typeReg = /tp:([a-zA-Z]*)#/;

export default {
	name: 'ContentArea',
	props: {
		postId: Number,
		showFullText: Boolean
	},
	components: {
		AdminOpsRecord,
		GalleryItemContent,
		PostImageList,
		Ellipsis
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	data() {
		const post = global.states.postManager.get({
			itemId: this.postId
		});
		return {
			post,
			fullUrlList: [],
			imgUrlList: [],
			imageBoxList: [],
			postContent: post.customContent || post.content,
			payload: '',
			contentType: 'normal',
			fullTextType: 'normal',
			fullText: '',
			isShowFullText: false,
			galleryItem: {}
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	watch: {
		'post.content': {
			immediate: true,
			handler() {
				this.resolveContentChange();
			}
		},
		'post.customContent': {
			immediate: true,
			handler() {
				this.resolveContentChange();
			}
		},
		'post.raw_img': {
			immediate: true,
			handler() {
				this.parseRawImg();
			}
		},
		'post.parsedImages': {
			immediate: true,
			handler() {
				// 已经有解析过的图片列表，则将其加入帖子的图片列表中
				// 此种情况是相册目录在移动端打开时先进入帖子详情，然后再打开图片列表
				if (this.post.parsedImages) {
					this.imgUrlList = [];
					this.fullUrlList = [];
					this.imageBoxList = [];
					this.parseRawImg();
					this.post.parsedImages.forEach(img => {
						this.imgUrlList.push(img.thumb);
						this.fullUrlList.push(img.src);
						this.imageBoxList.push(img)
					});
				}
			}
		},
		'isShowFullText' : {
			immediate: true,
			handler(isShow) {
				if (this.contentType === 'gallery') {
					this.fullTextType = 'gallery';
					if (isShow) {
						this.post.parsedImages = global.methods.parseGalleryItemFullText(this.post, this.post.full_text);
					} else {
						this.post.parsedImages = [];
					}
				}
			}
		}
	},
	computed: {
		hasFullText() {
			return (this.post.full_text_id || this.post.full_text) && !this.post.noFullText;
		},
		clientWidth() {
			// 巨坑，深扒van-tabs组件发现是在swipe组件中获取了一个tabs的宽度
			// 但是如果tabs组件不占满屏幕，又没有固定的px值，则返回0
			// tabs组件错乱
			if (this.lptContainer === 'blogDrawer') {
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
		showPostDetail(event) {
			const nodeName = event.target.nodeName;
			// 点击a标签不进入帖子详情
			if (nodeName === 'a' || nodeName === 'A') {
				return;
			}
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
		parseRawImg() {
			const rawImg = this.post.raw_img || '';
			const imgListStr = rawImg.split('#');
			imgListStr.forEach(img => {
				if (!img) return;
				const fullImgUrl = lpt.getFullImgUrl(img, Math.floor(this.clientWidth));
				this.imgUrlList.push(lpt.getPostThumbUrl(img));
				this.fullUrlList.push(fullImgUrl);
				this.imageBoxList.push({
					thumb: lpt.getPostThumbUrl(img),
					src: fullImgUrl
				});
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
		resolveContentChange() {
			if (this.showFullText && this.hasFullText) this.showFullTextFun();
			if (typeReg.test(this.post.content)) {
				this.contentType = this.post.content.match(typeReg)[1];
				const content = this.post.customContent || this.post.content;
				this.postContent = content.replace(typeReg, '');
			} else {
				if (global.methods.checkPostIsGalleryItem(this.post)) {
					this.contentType = 'gallery';
					this.galleryItem = global.methods.parseGalleryItemContent(this.post);
				}
				this.postContent = this.post.customContent || this.post.content;
			}
			this.$nextTick(() => {
				if (this.isShowFullText) {
					if (this.fullTextType === 'md') {
						const fullTextMd = this.$refs.fullTextMd;
						global.methods.parseContentElement({
							el: fullTextMd.$el,
							router: this.$router
						});
						translateMd({
							el: fullTextMd.$el,
							router: this.$router
						});
					} else if (this.fullTextType === 'normal') {
						const contentElem = this.$refs.normalFullText;
						global.methods.parseContentElement({
							el: contentElem,
							router: this.$router
						});
					}
				} else {
					if (this.contentType === 'md') {
						const contentMd = this.$refs.contentMd;
						global.methods.parseContentElement({
							el: contentMd.$el,
							router: this.$router
						});
						translateMd({
							el: contentMd.$el,
							router: this.$router
						});
					}
				}
			});
		},
		processFullText(fullText) {
			global.states.postManager.add({
				...this.post,
				full_text: fullText
			});
			if (typeReg.test(fullText)) {
				this.fullTextType = fullText.match(typeReg)[1];
				this.fullText = fullText.replace(typeReg, '');
			} else {
				this.fullTextType = this.contentType;
				this.fullText = fullText;
			}
			if (this.fullTextType === 'amOps') {
				this.processAmOps(fullText);
			}
		},
		showFullTextFun() {
			if (this.fullText || this.post.customFullText) {
				this.isShowFullText = true;
			} else if (this.post.full_text) {
				this.processFullText(this.post.full_text);
				this.isShowFullText = true;
			} else {
				lpt.postServ.getFullText({
					consumer: this.lptConsumer,
					param: {
						fullTextId: this.post.full_text_id
					},
					success: (result) => {
						this.processFullText(result.object);
						this.isShowFullText = true;
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
.content-area {
	padding: 0 2rem;
}

.content {
	text-align: left;
	word-break: break-all;
	font-size: 0.9rem;
	white-space: pre-wrap;
	margin-bottom: 1rem;
}

:global(.github-markdown-body) {
	font-size: 0.9rem;
}

.md-content {
	white-space: normal;
}

.md-content.abstract {
	margin-bottom: 0;
}

:global(.md-content .github-markdown-body) {
	padding: 1rem 0 2rem;
}

:global(.md-content.abstract .github-markdown-body) {
	/* 摘要中的markdown要尽量缩小空间 */
	padding: 0;
}

:global(.v-md-editor-preview) {
	padding: 0 !important;
}

.title {
	text-align: center;
	font-weight: bold;
	word-wrap: break-word;
	word-break: normal;
}

.full-text-btn {
	cursor: pointer;
	color: #007bff;
	font-size: 0.9rem;
}

:global(.vue-lb-info) {
	/*bottom: -20px !important;*/
	display: none;
}

:global(.v-md-editor-preview .nav-b) {
	display: flex;
	justify-content: space-around;
	width: 100%;
}

:global(.v-md-editor-preview .nav-i) {
	font-size: 1.5rem;
	font-weight: bold
}

:global(.github-markdown-body table) {
	/* 不然markdown中的表格会居左显示 */
	display: table;
}
</style>