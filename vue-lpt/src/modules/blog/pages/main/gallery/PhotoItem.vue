<template>
	<div class="photo-item" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave">
		<img ref="img" :src="coverUrl" v-show="post.settled" @click="showPreview" :onload="onImgLoaded"
		     :onerror="onImgLoaded" ondragstart="return false"/>
		<image-box ref="imageBox" :images="images" :show-image-list="false"
		           :containerStyle="{width: '67%', marginLeft: '33%'}"/>
		<bottom-bar class="bottom-bar" v-show="showBottomBar"
		            :post-id="post.id" :post-of="postOf" :show-actions="true"
		            @click.capture.stop="showPreview"/>
		<a-row class="photo-num" v-show="showBottomBar">
			<a-col>
				<ri-stack-line class="icon"/>
			</a-col>
			<a-col>
				<p class="num">{{ photoNum }}张</p>
			</a-col>
		</a-row>
	</div>
</template>

<script>
import global from "@/lib/js/global";
import ImageBox from '@/components/global/ImageBox';
import BottomBar from "@/components/post/item/parts/BottomBar";
import {RiStackLine} from "@/assets/icons/remix-icon";
import {watch} from "vue";
import lpt from "@/lib/js/laputa/laputa";

const contentReg = /描述：([\s\S]*)\n封面：([\s\S]*)\n张数：([\s\S]*)/;
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
	data() {
		const post = global.states.postManager.get({
			itemId: this.postId,
			success: (post) => {
				this.$nextTick(() => {
					const res = contentReg.exec(post.content);
					if (!res || res.length < 4) {
						console.warn('图片贴解析错误，错误帖子对象如下：');
						console.warn(post);
						return;
					}
					this.desc = res[1];
					this.coverUrl = res[2];
					this.photoNum = parseInt(res[3]);
					this.post.customContent = this.desc;
				});
			}
		});
		return {
			post,
			coverUrl: '',
			desc: '',
			photoNum: 0,
			images: [],
			showBottomBar: false,
		};
	},
	mounted() {
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
	},
	methods: {
		onMouseEnter() {
			if (!global.states.blog.showDrawer) this.showBottomBar = true;
		},
		onMouseLeave() {
			if (!global.states.blog.showDrawer) this.showBottomBar = false;
		},
		showPreview() {
			if (typeof this.post.full_text_id === 'undefined') return;
			this.showBottomBar = false;
			this.post.noFullText = true;
			if (!this.hasLoadedFullText) {
				const promise = lpt.postServ.getFullText({
					consumer: this.lptConsumer,
					param: {
						fullTextId: this.post.full_text_id
					},
					success: (result) => {
						this.hasLoadedFullText = true;
						const fullText = result.object;
						const parts = fullText.split(/\-+分割线\-+/);
						let imgListStr = '';
						if (parts.length > 1) {
							imgListStr = parts[parts.length - 1].trim();
							this.post.customFullText = parts[0];
							this.post.noFullText = false;
						} else {
							imgListStr = fullText;
						}
						const list = imgListStr.split('\n');
						list.forEach((img) => {
							const parts = img.split(';');
							if (parts.length === 0) return;
							this.images.push({
								src: parts[0],
								thumb: parts.length > 1 ? parts[1] : undefined
							});
						});
					}
				});
				this.$refs.imageBox.preShow(promise);
			} else {
				this.$refs.imageBox.show();
			}
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
	bottom: 1rem;
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
</style>