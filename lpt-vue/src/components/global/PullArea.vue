<template>
	<div style="height: 100%">
		<top-area :pull-distance="pullDistance.down" :pull-finish="pullFinish.down"
		          :finish-callback="resetTouch"></top-area>
		<div @touchstart="touchStart($event)"
		     @touchmove="touchMove($event)" @touchend="touchEnd($event)">
			<slot></slot>
		</div>
	</div>
</template>

<script>
import TopArea from './hide_areas/TopArea';

export default {
	name: 'PullArea',
	components: {
		TopArea
	},
	data() {
		this.currentDirection = '';
		this.directions = ['down', 'up', 'left', 'right'];
		this.pullLimit = {
			down: 100,
			up: 100,
			left: 100,
			right: 100
		};
		const pullDistance = {};
		const pullFinish = {};
		this.directions.forEach((dir) => {
			pullDistance[dir] = 0;
			pullFinish[dir] = false;
		});
		return {
			pullDistance,
			pullFinish
		}
	},
	methods: {
		resetTouch() {
			const curDir = this.currentDirection;
			this.directions.forEach((dir) => {
				this.pullDistance[dir] = 0;
			});
			this.pullFinish[curDir] = false;
		},
		touchStart(event) {
			this.touchStartY = event.targetTouches[0].pageY;
			this.touchStartX = event.targetTouches[0].pageX;
		},
		touchMove(event) {
			const curTarget = event.currentTarget;
			const curScrollTop = curTarget.scrollTop;
			const curScrollLeft = curTarget.scrollLeft;
			const curClientHeight = curTarget.clientHeight;
			const curClientWidth = curTarget.clientWidth;
			const touchCurrentY = event.targetTouches[0].pageY;
			const touchCurrentX = event.targetTouches[0].pageX;
			const diffY = touchCurrentY - this.touchStartY;
			const diffX = touchCurrentX - this.touchStartX;
			if (!this.currentDirection) {
				if (Math.abs(diffY) > Math.abs(diffX)) {
					if (diffY > 0 && curScrollTop === 0) {
						// 向下滑并且当前滚动条在顶端
						this.currentDirection = 'down';
					} else if (diffY < 0 && curScrollTop === curClientHeight) {
						// 向上滑并且当前滚动条在底端
						this.currentDirection = 'up';
					}
				} else {
					if (diffX > 0 && curScrollLeft === 0) {
						// 向右滑并且当前滚动条在左端
						this.currentDirection = 'right';
					} else if (curScrollLeft === curClientWidth) {
						// 向左滑并且当前滚动条在右端
						this.currentDirection = 'left';
					}
				}
			}
			const curDir = this.currentDirection;
			if (!curDir) {
				return;
			}
			if (curDir === 'down') {
				// 阻止浏览器默认下拉刷新操作
				event.preventDefault();
			}
			let moveDist;
			if (curDir === 'down' || curDir === 'up') {
				moveDist = Math.abs(diffY);
			} else {
				moveDist = Math.abs(diffX);
			}
			if (moveDist <= this.pullLimit[curDir]) {
				this.pullDistance[curDir] = moveDist;
			} else {
				// 超过滑动界限，不再更新距离
				// 注意，滑动回调不是实时的，两个moveDist之间可能差几个像素
				// 所以超过滑动界限后直接将滑动距离赋值为滑动界限即可
				this.pullDistance[curDir] = this.pullLimit[curDir];
			}
		},
		touchEnd() {
			const curDir = this.currentDirection;
			this.currentDirection = '';
			if (this.pullDistance[curDir] < this.pullLimit[curDir]) {
				this.resetTouch();
				return;
			}
			this.pullFinish[curDir] = true;
		}
	}
}
</script>

<style scoped>

</style>