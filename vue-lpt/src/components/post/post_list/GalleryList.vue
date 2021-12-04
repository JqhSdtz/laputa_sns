<template>
	<post-list ref="postList" :category-id="category.id" :top-post-id="category.top_post_id"
					@refresh="$emit('refresh');"
					:clear-on-refresh="true"
					@loaded="$emit('loaded');"
		           :sort-type="sortType"
		           :custom-load-process="processImgLoad"
		           :on-batch-processed="onBatchProcessed"
				   style="padding: 1rem 0.5rem 0 0.5rem">
			<template v-slot:default="sProps">
				<a-row id="photo-container" type="flex" justify="space-around">
					<a-col v-for="post in sProps.postList" :key="post.id">
						<photo-item class="photo-item" :ref="'photo' + post.id" 
								:post-id="post.id"
								:post-of="postOf"
								:is-top-post="post.id === sProps.topPostId"
								@beforeShowPreview="$emit('beforeShowPreview', post)"
								@afterShowPreview="$emit('afterShowPreview', post)"
						        :on-img-loaded="() => onImgLoaded(post)"/>
					</a-col>
				</a-row>
			</template>
	</post-list>
</template>

<script>
/**
 * 说明：getRatio,setRow,arrangeList等与图片排版有关的代码借鉴自图虫网
 */
import global from '@/lib/js/global';
import remHelper from '@/lib/js/uitls/rem-helper';
import {Toast} from 'vant';
import PhotoItem from "@/components/post/item/PhotoItem";
import PostList from "@/components/post/post_list/PostList";

export default {
	name: 'GalleryList',
	components: {
		PostList,
		PhotoItem
	},
	props: {
		postOf: {
			type: String,
			default: 'category'
		},
		categoryId: {
			type: Number,
			default: 0
		},
		sortType: {
			type: String,
			default: 'popular'
		},
		topPostId: Number,
	},
	emits: ['refresh', 'loaded'],
	inject: {
		lptContainer: {
			type: String
		}
	},
	data() {
		const category = global.states.categoryManager.get({
			itemId: this.categoryId,
			fail(result) {
				Toast.fail(result.message);
			}
		});
		return {
			category,
			enableBoolMode: false,
			searchValue: '',
			mainBarHeight: global.vars.style.tabBarHeight
		}
	},
	created() {
		this.imgLoadResolveMap = new Map();
		window.addEventListener('resize', () => {
			this.curListArrange && this.curListArrange();
		});
	},
	methods: {
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
			// 左右的padding加起来是1rem
			const width = this.$el.clientWidth - remHelper.remToPx(1);
			this.curListArrange = () => this.arrangeList(width, imgList, 12);
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
			// lptContainer为default表示为移动端界面，缩小最小高度
			const minHeight = this.lptContainer === 'blogMain' ? 250 : 125;
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
#photo-container {
	height: 100%;
	overflow-y: visible;
	background-color: white;
}

.photo-item {
	margin-bottom: 1rem;
}
</style>