<template>
	<div class="main-area" :class="{'with-scroll-bar': lptContainer === 'blogMain'}"
	     :style="{height: scrollHeight, position: 'relative'}" keep-scroll-top v-scroll-view>
		<sort-type-selector v-if="postListLoaded" v-model:sort-type="sortType" position="left"
			offset="2.5rem"/>
		<a-back-top :style="{bottom: (mainBarHeight + 10) + 'px'}" :target="getElement"/>
		<post-list ref="postList" :category-id="category.id" :top-post-id="category.top_post_id"
		           :sort-type="sortType" @loaded="onPostListLoaded"
		           :custom-load-process="processImgLoad"
		           :on-batch-processed="onBatchProcessed"
				   style="padding-top: 0.35rem">
			<template v-slot:default="sProps">
				<a-row id="photo-container" type="flex" justify="space-around">
					<a-col v-for="post in sProps.postList" :key="post.id">
						<photo-item class="photo-item" :post-id="post.id"
						            :ref="'photo' + post.id"
						            :is-top-post="post.id === sProps.topPostId"
						            :on-img-loaded="() => onImgLoaded(post)"/>
					</a-col>
				</a-row>
			</template>
		</post-list>
	</div>
</template>

<script>
/**
 * 说明：getRatio,setRow,arrangeList等与图片排版有关的代码借鉴自图虫网
 */
import global from '@/lib/js/global';
import blogDescription from '@/modules/blog/description';
import {Toast} from 'vant';
import SortTypeSelector from '@/components/post/post_list/SortTypeSelector';
import PhotoItem from "@/modules/blog/pages/main/gallery/PhotoItem";
import PostList from "@/components/post/post_list/PostList";

export default {
	name: 'Gallery',
	components: {
		PostList,
		PhotoItem,
		SortTypeSelector
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	data() {
		const category = global.states.categoryManager.get({
			itemId: blogDescription.galleryCategoryId,
			fail(result) {
				Toast.fail(result.message);
			}
		});
		const sortType = localStorage.getItem('sortTypeGallery') || 'latest';
		return {
			category,
			enableBoolMode: false,
			searchValue: '',
			mainBarHeight: global.vars.style.tabBarHeight,
			sortType: sortType,
			postListLoaded: false
		}
	},
	watch: {
		sortType(value) {
			localStorage.setItem('sortTypeGallery', value);
		}
	},
	computed: {
		scrollHeight() {
			if (this.lptContainer === 'blogMain') {
				return global.states.style.mainHeight + 'px';
			}
			const mainViewHeight = global.states.style.bodyHeight;
			// 底部高度加0.5的padding
			let barHeight = this.mainBarHeight;
			return mainViewHeight - barHeight + 'px';
		}
	},
	created() {
		this.imgLoadResolveMap = new Map();
		window.addEventListener('resize', () => {
			this.curListArrange && this.curListArrange();
		});
	},
	activated() {
		global.methods.setTitle({
			pageDesc: '相册',
			route: this.$route
		});
	},
	methods: {
		getElement() {
			return this.$el;
		},
		onPostListLoaded() {
			this.postListLoaded = true;
		},
		processImgLoad(post) {
			post.settled = false;
			return new Promise(resolve => {
				this.imgLoadResolveMap.set(post.id, resolve);
			});
		},
		onImgLoaded(post) {
			const resolve = this.imgLoadResolveMap.get(post.id);
			resolve();
		},
		onBatchProcessed(newList, fullList) {
			const imgList = [];
			for (let i = 0; i < fullList.length; ++i) {
				let post = fullList[i];
				const photo = this.$refs['photo' + post.id];
				const img = photo.$refs.img;
				post = global.states.postManager.get({
					itemId: post.id
				});
				imgList.push({
					el: img,
					width: img.width,
					height: img.height,
					post: post
				});
			}
			this.curListArrange = () => this.arrangeList(this.$el.clientWidth, imgList, 12);
			this.curListArrange();
		},
		getRatio(width, height) {
			let ratio = width / height;
			if (ratio < 0.75) {
				ratio = 0.75;
			} else if (ratio > 5 / 3) {
				ratio = 5 / 3;
			}
			return ratio;
		},
		setRow(curRow, rowHeight) {
			curRow.forEach((img) => {
				img.el.style.height = rowHeight + 'px';
				img.el.style.width = Math.floor(rowHeight * img.ratio) + 'px';
				img.post.settled = true;
			});
		},
		arrangeList(containerWidth, imgList, rangeMargin) {
			const minHeight = 250;
			const listLength = imgList.length;
			let curRow = [];
			let ratioSum = 0
			let rowHeight = 0;
			imgList.forEach((img, index) => {
				const width = img.width || 300;
				const height = img.height || 400;
				const ratio = this.getRatio(width, height);
				img.ratio = ratio;
				curRow.push(img);
				ratioSum += ratio;
				let curActualWidth = containerWidth - (curRow.length - 1) * rangeMargin;
				let curActualHeight = Math.floor(curActualWidth / ratioSum);
				if (curActualHeight <= minHeight || curRow.length > 3 && curActualHeight < 375) {
					const lastImg = curRow.pop();
					curActualWidth = containerWidth - (curRow.length - 1) * rangeMargin;
					ratioSum -= ratio;
					this.setRow(curRow, rowHeight);
					curRow = [lastImg];
					ratioSum = lastImg.ratio;
					curActualWidth = containerWidth;
					curActualHeight = Math.floor(curActualWidth / ratioSum);
				}
				rowHeight = curActualHeight;
				if (index + 1 === listLength) {
					if (curActualHeight > minHeight || curRow.length < 3) {
						curActualHeight = Math.min(curActualHeight, 400);
						curActualHeight = Math.round((minHeight + curActualHeight) / 2);
					}
					this.setRow(curRow, curActualHeight);
				}
			});
		}
	}
}
</script>

<style scoped>
.main-area {
	overflow-y: scroll;
}

#photo-container {
	height: 100%;
	overflow-y: visible;
}

.photo-item {
	margin-bottom: 1rem;
}
</style>