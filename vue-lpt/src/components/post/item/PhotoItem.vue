<template>
	<a-col v-show="post.settled">
		<div class="photo-item" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave">
			<img ref="img" :src="coverUrl" @click="showPreview" 
				:onload="onImgLoaded" :onerror="onImgLoaded" ondragstart="return false"/>
			<image-box v-if="lptContainer !== 'default'" ref="imageBox" :images="images" :show-image-list="false"
				:containerStyle="{width: '67%', marginLeft: '33%'}"/>
			<bottom-bar class="bottom-bar" v-show="showBottomBar"
				:post-id="post.id" :post-of="postOf" :show-actions="true"
				:on-show-post-detail="onShowPostDetail"
				@click.capture.self="showPreview"/>
			<a-row class="photo-num" v-show="showBottomBar">
				<a-col>
					<ri-stack-line class="icon"/>
				</a-col>
				<a-col>
					<p class="num">{{ photoNum }}张</p>
				</a-col>
				<a-col v-if="isTopPost" class="topped-tag">
					<van-tag  type="primary">
						置顶
					</van-tag>
				</a-col>
			</a-row>
		</div>
	</a-col>
</template>

<script>
import global from "@/lib/js/global";
import ImageBox from '@/components/global/ImageBox';
import BottomBar from "@/components/post/item/parts/BottomBar";
import {RiStackLine} from "@/assets/icons/remix-icon";
import {watch} from "vue";
import lpt from "@/lib/js/laputa/laputa";

export default {
	name: 'PhotoItem',
	props: {
		postId: Number,
		postOf: String,
		isTopPost: Boolean,
		onImgLoaded: Function
	},
	components: {
		ImageBox,
		BottomBar,
		RiStackLine
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	data() {
		const post = global.states.postManager.get({
			itemId: this.postId,
			success: (post) => {
				this.$nextTick(() => {
					const galleryItem = global.methods.parseGalleryItemContent(post);
					if (!galleryItem) return;
					this.desc = galleryItem.desc;
					this.coverUrl = galleryItem.coverUrl;
					this.photoNum = galleryItem.photoNum;
				});
			}
		});
		const imgStyle = {};
		return {
			post,
			imgStyle,
			coverUrl: '',
			desc: '',
			photoNum: 0,
			images: [],
			showBottomBar: false,
		};
	},
	mounted() {
		if (this.lptContainer !== 'default') {
			watch(() => this.$refs.imageBox.state.show, (curShow) => {
				if (!curShow) {
					global.states.blog.showDrawer = false;
				} else {
					this.$router.push({
						path: '/post_detail/' + this.post.id
					});
					global.states.blog.showDrawer = true;
					// drawer缩回去，不然只会显示一半
					global.methods.setDrawerWidth('33%');
				}
			});
		}
	},
	methods: {
		onMouseEnter() {
			if (!global.states.blog.showDrawer) this.showBottomBar = true;
		},
		onMouseLeave() {
			if (!global.states.blog.showDrawer) this.showBottomBar = false;
		},
		onShowPostDetail() {
			this.showPreview();
			// 取消默认操作，即不跳转到详情页
			return false;
		},
		showPreview() {
			if (typeof this.post.full_text_id === 'undefined') return;
			this.showBottomBar = false;
			this.doShowPreview();
		},
		doShowPreview() {
			if (!this.hasLoadedFullText) {
				const promise = lpt.postServ.getFullText({
					consumer: this.lptConsumer,
					param: {
						fullTextId: this.post.full_text_id
					},
					success: (result) => {
						this.hasLoadedFullText = true;
						const fullText = result.object;
						this.images = global.methods.parseGalleryItemFullText(this.post, fullText);
						if (this.lptContainer === 'default') {
							this.post.parsedImages = this.images;
							global.states.postManager.add(this.post);
						}
					}
				});
				if (this.lptContainer !== 'default') {
					this.$refs.imageBox.preShow(promise);
				} else {
					this.showVantImagePreview();
				}
			} else {
				if (this.lptContainer !== 'default') {
					this.$refs.imageBox.show();
				} else {
					this.showVantImagePreview();
				}
			}
		},
		showVantImagePreview() {
			this.$router.push({
				path: '/post_detail/' + this.post.id
			});
		}
	}
}
</script>

<style scoped>
.photo-item {
	display: inline-block;
}

img {
	object-fit: cover;
	user-select: none;
}

.bottom-bar {
	width: 100%;
	background-color: white;
	position: absolute;
	bottom: 0;
}

.photo-num {
	position: absolute;
	top: 0;
	width: 100%;
	background-color: white;
	padding: 0.5rem;
}

.photo-num .icon {
	margin-left: 0.5rem;
	font-size: 1.25rem;
}

.photo-num .num {
	margin: 0;
	margin-left: 0.25rem;
	font-size: 0.85rem;
}

.topped-tag {
	float: left;
	margin-left: 1rem;
}
</style>