<template>
	<div class="content-area">
		<p class="title" @click="showPostDetail">{{ post.title }}</p>
		<div v-if="isShowFullText" class="content" :class="{'md-content': fullTextType === 'md'}">
			<p ref="normalFullText" v-if="fullTextType === 'normal'" style="margin-bottom: 0;">{{ fullText }}</p>
			<admin-ops-record v-if="fullTextType === 'amOps' && payload" :payload="payload"/>
			<v-md-preview ref="fullTextMd" v-if="fullTextType === 'md'" :text="fullText"/>
		</div>
		<div v-if="!isShowFullText" class="content" :class="{'md-content': contentType === 'md'}">
			<v-md-preview ref="contentMd" v-if="contentType === 'md'" :text="postContent"/>
			<ellipsis v-else :content="postContent" :rows="5"/>
		</div>
		<p v-if="hasFullText && !isShowFullText" class="full-text-btn" @click.stop="showFullTextFun">
			查看全文
		</p>
		<p v-if="isShowFullText" class="full-text-btn" @click.stop="hideFullText">
			收起全文
		</p>
		<div v-if="imgList.length > 0">
			<div v-if="env === 'lpt'">
				<a v-for="(img, idx) in imgList" :key="img" @click="showImgPreview(idx)"
				   style="display: inline; margin-right: 1rem;">
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
import {translateMd} from '@/lib/js/markdown/md-translator';
import CategoryPath from '@/components/category/CategoryPath';
import {ImagePreview} from 'vant';
import AdminOpsRecord from '@/components/post/item/parts/AdminOpsRecord';
import ImageBox from '@/components/global/ImageBox';
import Ellipsis from '@/components/global/Ellipsis';

const typeReg = /tp:([a-zA-Z]*)#/;

export default {
	name: 'ContentArea',
	props: {
		post: Object,
		showFullText: Boolean
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
			contentType: 'normal',
			fullTextType: 'normal',
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
		},
		'post.parsedImages': {
			immediate: true,
			handler() {
				// 已经有解析过的图片列表，则将其加入帖子的图片列表中
				// 此种情况是相册目录在移动端打开时先进入帖子详情，然后再打开图片列表
				if (this.post.parsedImages) {
					this.post.parsedImages.forEach(img => {
						this.imgList.push(img.thumb);
						this.fullUrlList.push(img.src);
					});
				}
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
		hasFullText() {
			return (this.post.full_text_id || this.post.full_text) && !this.post.noFullText;
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
				startPosition: index,
				overlayStyle: {
					backgroundColor: 'rgba(0, 0, 0, 0.75)',
					backdropFilter: 'blur(20px)'
				}
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
			if (this.fullText) {
				this.isShowFullText = true;
			} else if (this.post.customFullText) {
				this.fullText = this.post.customFullText;
				this.isShowFullText = true;
			} else if (this.post.full_text) {
				this.isShowFullText = true;
				this.processFullText(this.post.full_text);
			} else {
				lpt.postServ.getFullText({
					consumer: this.lptConsumer,
					param: {
						fullTextId: this.post.full_text_id
					},
					success: (result) => {
						this.isShowFullText = true;
						this.processFullText(result.object);
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

.md-content {
	white-space: normal;
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
}

:global(.image-box-list img) {
	display: inline;
	margin-right: 1rem;
}

:global(.image-box-list>div>div) {
	display: inline-block;
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
</style>