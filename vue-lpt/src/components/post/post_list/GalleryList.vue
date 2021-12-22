<template>
	<post-list ref="postList" :category-id="category.id" :top-post-id="category.top_post_id"
					@refresh="onRefresh"
					:clear-on-refresh="true"
					@loaded="$emit('loaded');"
					@finish="finished = true"
		           :sort-type="sortType"
		           :custom-load-process="processImgLoad"
		           :on-batch-processed="onBatchProcessed"
				   style="background-color: white;padding: 1rem 0.5rem 0 0.5rem">
			<template v-slot:default="sProps">
				<a-row id="photo-container" type="flex" justify="space-around">
					<photo-item v-for="post in sProps.postList" :key="post.id" class="photo-item" :ref="'photo' + post.id" 
							:post-id="post.id"
							:post-of="postOf"
							:is-top-post="post.id === sProps.topPostId"
							@beforeShowPreview="$emit('beforeShowPreview', post)"
							@afterShowPreview="$emit('afterShowPreview', post)"
							:on-img-loaded="() => onImgLoaded(post)"/>
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

const rangeMargin = 12;

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
			// 保存一行的图片
			curRow: [],
			finished: false,
			enableBoolMode: false,
			searchValue: '',
			mainBarHeight: global.vars.style.tabBarHeight
		}
	},
	watch: {
		sortType() {
			this.finished = false;
			this.curRow = [];
		}
	},
	created() {
		this.imgLoadResolveMap = new Map();
		window.addEventListener('resize', () => {
			if (this.fullImgList) {
				const width = this.$el.clientWidth - remHelper.remToPx(1);
				this.curRow = [];
				this.arrangeList(width, this.fullImgList, 12);
			}
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
		processPostList(postList) {
			const imgList = [];
			for (let i = 0; i < postList.length; ++i) {
				let post = postList[i];
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
			return imgList;
		},
		onBatchProcessed(newList, fullList) {
			const newImgList = this.processPostList(newList);
			this.fullImgList = this.processPostList(fullList);
			// 左右的padding加起来是1rem
			const width = this.$el.clientWidth - remHelper.remToPx(1);
			this.arrangeList(width, newImgList);
		},
		onRefresh() {
			this.$emit('refresh');
			this.finished = false;
			this.curRow = [];
		},
		getRatio(width, height) {
			let ratio = width / height;
			if (ratio < 0.75) {
				// 图片的宽高比若低于3:4，则设为3:4，即不能太高
				ratio = 0.75;
			} else if (ratio > 5 / 3) {
				// 图片的宽高比大于5:3，则设为5:3，即不能太宽
				ratio = 5 / 3;
			}
			return ratio;
		},
		setRow(rowHeight) {
			this.curRow.forEach((img) => {
				// 高度设置为排版后的该行的高度
				img.el.style.height = rowHeight + 'px';
				// 根据过滤过的宽高比重设宽度，即相当于在缩放的同时做剪裁
				img.el.style.width = Math.floor(rowHeight * img.ratio) + 'px';
				img.post.settled = true;
			});
		},
		calActualHeight(containerWidth, imgNum = this.curRow.length) {
			// 宽高比总和，用于辅助判断一行图片是否放得下
			let ratioSum = 0;
			for (let i = 0; i < imgNum; ++i) {
				const img = this.curRow[i];
				ratioSum += img.ratio;
			}
			// 容器宽度减去当前该行已放置图片之间的margin，即该行如果仅放置curRow中的图片，图片的实际占用宽度
			// 如果只放一张照片，就减去一个rangeMargin。如果不减，则宽度会和上面的行不匹配，应该是弹性布局的原因，还未深究
			const curActualWidth = containerWidth - ((this.curRow.length - 1) || 1) * rangeMargin;
			// 计curActualHeight为h，curActualWidth为w，ratioSum=r1+r2+r3
			// 则w=h*r1+h*r2+h*r3，即h为预计的该行实际高度
			return Math.floor(curActualWidth / ratioSum);
		},
		arrangeList(containerWidth, imgList) {
			// lptContainer为default表示为移动端界面，缩小最小高度
			const minHeight = this.lptContainer === 'blogMain' ? 250 : 125;
			const maxHeight = 375;
			// 如果一行放3个图片可以满足要求，就不再放第4个
			const standardNumPerRow = 3;
			imgList.forEach(img => {
				const width = img.width || 300;
				const height = img.height || 400;
				const ratio = this.getRatio(width, height);
				img.ratio = ratio;
				this.curRow.push(img);
				// 上一次计算的实际高度，即没有该图片时的实际高度
				const preActualHeight = this.calActualHeight(containerWidth, this.curRow.length - 1);
				// 当前实际高度
				const curActualHeight = this.calActualHeight(containerWidth);
				// 排版策略为，一个一个图片往上放，然后直到发现这一行放得过多了，然后去掉最后一个，即作为该行最后的排版
				// 那么什么情况下说明该行放的图片过多了呢？一是该行高度已被挤得小于最小高度了，
				// 二是该行的图片数量大于标准图片数量并且去掉最后一个图片后高度已经小于最大高度，即不需要通过增加数量来减小高度
				if (curActualHeight <= minHeight 
						|| this.curRow.length > standardNumPerRow && preActualHeight < maxHeight) {
					// 当图片过多时，就去掉一个最后一个图片
					const lastImg = this.curRow.pop();
					// 然后以上一个实际高度作为最终的实际高度
					this.setRow(preActualHeight);
					// 然后初始化下一行为仅包含该图片的行
					this.curRow = [lastImg];
				}
			});
			if (this.finished) {
				console.log('finished!!!')
				// 如果所有图片都已经加载完，则对最后剩下的图片做单独排版
				let curActualHeight = this.calActualHeight(containerWidth);
				curActualHeight = Math.min(curActualHeight, maxHeight);
				this.setRow(curActualHeight);
			}
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