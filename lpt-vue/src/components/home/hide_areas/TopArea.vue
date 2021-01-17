<template>
	<div id="top-area" v-if="pullDistance > 0" :style="{height: pullDistance + 'px'}">
		<p>下拉刷新</p>
	</div>
</template>

<script>
export default {
	name: 'TopArea',
	props: {
		pullDistance: Number,
		pullFinish: Boolean,
		finishCallback: Function
	},
	inject: ['refreshMainView'],
	methods: {
		refresh() {
			this.refreshMainView();
		}
	},
	watch: {
		pullFinish(newValue, oldValue) {
			if (newValue && !oldValue) {
				this.finishCallback();
				this.refresh();
			}
		}
	}
}
</script>

<style scoped>
#top-area {
	/* 如果要改变顶部区域显示方式，例如在额外的区域显示，
		就像支付宝那种，则改变这里的样式即可 */
	z-index: 2;
	position: fixed;
	overflow-y: hidden;
	width: 100%;
	background-color: white;
}

p {
	position: absolute;
	font-size: 20px;
	font-weight: bold;
	transform: translate(-50%, -50%);
	left: 50%;
	top: 50%;
}
</style>