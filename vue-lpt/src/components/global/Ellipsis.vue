<template>
	<div class="mm-ellipsis-container">
		<div class="shadow">
			<textarea :rows="rows" readonly></textarea>
			<div class="shadow-box" ref="box">
				{{ showContent }}
				<slot name="ellipsis">
					{{ ellipsisText }}
					<span class="ellipsis-btn">{{ btnText }}</span>
				</slot>
				<!-- 这里有修改，原来是<span ref="tail"></span>，但是在微信内置浏览器中
				 空的行内元素会定位到元素的左上角，并不会像预期的跟随在内容最后，只有设为
				  inline-block的块级元素才会在空的情况下跟随内容 -->
				<div ref="tail" style="display: inline-block"></div>
			</div>
		</div>
		<div v-if="showFullText" class="ellipsis-content">
			{{ content }}
			<span class="ellipsis-btn" @click="toggle">收起</span>
		</div>
		<div v-else class="real-box">
			{{ showContent }}
			<slot name="ellipsis" v-if="(textLength < content.length) || btnShow">
				{{ ellipsisText }}
				<span class="ellipsis-btn" @click="toggle">{{ btnText }}</span>
			</slot>
		</div>
	</div>
</template>

<script>
//https://github.com/Lushenggang/vue-overflow-ellipsis/blob/main/src/components/ellipsis.vue

import resizeObserver from 'element-resize-detector'

const observer = resizeObserver();

export default {
	name: 'Ellipsis',
	props: {
		content: {
			type: String,
			default: ''
		},
		btnText: {
			type: String,
			default: '展开'
		},
		ellipsisText: {
			type: String,
			default: '...'
		},
		rows: {
			type: Number,
			default: 3
		},
		btnShow: {
			type: Boolean,
			default: false
		},
	},
	data() {
		return {
			showFullText: false,
			textLength: 0,
			beforeRefresh: null,
			boxWidth: 0,
			boxHeight: 0
		}
	},
	computed: {
		showContent() {
			return this.content.substr(0, this.textLength);
		},
		watchData() {
			return [this.content, this.btnText, this.ellipsisText, this.rows, this.btnShow];
		}
	},
	watch: {
		watchData: {
			immediate: true,
			handler() {
				this.refresh();
			}
		},
	},
	mounted() {
		observer.listenTo(this.$refs.box, (el) => {
			if (el.offsetWidth == this.boxWidth && el.offsetHeight == this.boxHeight) return;
			this.boxWidth = el.offsetWidth;
			this.boxHeight = el.offsetHeight;
			this.refresh();
		})
	},
	beforeUnmount() {
		observer.uninstall(this.$refs.box);
	},
	methods: {
		toggle() {
			this.showFullText = !this.showFullText;
		},
		refresh() {
			this.beforeRefresh && this.beforeRefresh();
			let stopLoop = false;
			this.beforeRefresh = () => stopLoop = true;
			this.textLength = this.content.length;
			const checkLoop = (start, end) => {
				if (stopLoop || start + 1 >= end) {
					this.beforeRefresh = null;
					return;
				}
				const boxRect = this.$refs.box.getBoundingClientRect();
				const tailRect = this.$refs.tail.getBoundingClientRect();
				const overflow = tailRect.bottom > boxRect.bottom;
				overflow ? (end = this.textLength) : (start = this.textLength);
				this.textLength = Math.floor((start + end) / 2);
				this.$nextTick(() => checkLoop(start, end));
			}
			this.$nextTick(() => checkLoop(0, this.textLength));
		}
	}
}
</script>

<style scoped>
.mm-ellipsis-container {
	text-align: left;
	position: relative;
	/*line-height: 1.5rem;*/
}

.mm-ellipsis-container .shadow {
	width: 100%;
	display: flex;
	pointer-events: none;
	opacity: 0;
	user-select: none;
	position: absolute;
	outline: green solid 1px;
}

textarea {
	border: none;
	flex: auto;
	padding: 0;
	resize: none;
	overflow: hidden;
	font-size: inherit;
	line-height: inherit;
	outline: none;
}

.mm-ellipsis-container .shadow-box {
	position: absolute;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
}

.ellipsis-btn {
	cursor: pointer;
	text-decoration: underline;
	color: #4791ff;
}
</style>