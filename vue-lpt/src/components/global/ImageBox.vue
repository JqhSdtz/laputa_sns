<template>
	<a-row v-if="showImageList" :class="imagesListClass">
		<a-col :span="lptContainer === 'blogMain' ? 3 : 8" v-for="(img, i) in images" :key="i">
			<img :src="img.thumb" style="object-fit: cover;" :style="thumbStyle" 
				@click="selected(i, img.src, img.caption)" 
				ondragstart="return false"/>
		</a-col>
	</a-row>
	<teleport to="body">
		<div ref="fullScreenContainer">
			<div v-if="state.show && state.isFullScreen" @mousemove="mouseMove"
				style="position: fixed; height: 100%; width: 100%;">
				<img class="vue-lb-modal-image"
					style="max-height: calc(100vh - 1px);
					position: fixed;
					top: 50%;
					left:50%;
					object-fit: contain;
					transform: translate(-50%, -50%)"
					:src="state.selectedImgPath" @touchstart="touchStart" @touchend="touchEnd"
					ondragstart="return false"
					@click="toggleFullScreen"/>
				<transition name="van-fade">
					<div v-show="state.showPageButton">
						<button type="button" title="Previous" class="vue-lb-arrow vue-lb-left" @click="prev">
					<span>
						<svg fill="white" x="0px" y="0px" width="100%" height="100%" viewBox="0 0 512 512">
							<path
								d="M213.7,256L213.7,256L213.7,256L380.9,81.9c4.2-4.3,4.1-11.4-0.2-15.8l-29.9-30.6c-4.3-4.4-11.3-4.5-15.5-0.2L131.1,247.9 c-2.2,2.2-3.2,5.2-3,8.1c-0.1,3,0.9,5.9,3,8.1l204.2,212.7c4.2,4.3,11.2,4.2,15.5-0.2l29.9-30.6c4.3-4.4,4.4-11.5,0.2-15.8 L213.7,256z"></path>
						</svg>
					</span>
						</button>
						<button type="button" title="Next" class="vue-lb-arrow vue-lb-right" @click="next">
					<span>
						<svg fill="white" x="0px" y="0px" width="100%" height="100%" viewBox="0 0 512 512"
							xml:space="preserve">
							<path
								d="M298.3,256L298.3,256L298.3,256L131.1,81.9c-4.2-4.3-4.1-11.4,0.2-15.8l29.9-30.6c4.3-4.4,11.3-4.5,15.5-0.2l204.2,212.7 c2.2,2.2,3.2,5.2,3,8.1c0.1,3-0.9,5.9-3,8.1L176.7,476.8c-4.2,4.3-11.2,4.2-15.5-0.2L131.3,446c-4.3-4.4-4.4-11.5-0.2-15.8 L298.3,256z"></path>
						</svg>
					</span>
						</button>
					</div>
				</transition>
			</div>
		</div>
		<div v-if="state.show" class="vue-lb-container" :style="containerStyle"
			@mousemove="mouseMove" ref="container">
			<div class="vue-lb-content">
				<div class="vue-lb-header">
					<span></span>
					<button type="button" title="Close (Esc)" class="vue-lb-button-close" @click="state.show = false">
						<span>
							<svg fill="white" x="0px" y="0px" width="100%" height="100%" viewBox="0 0 512 512">
								<path
									d="M443.6,387.1L312.4,255.4l131.5-130c5.4-5.4,5.4-14.2,0-19.6l-37.4-37.6c-2.6-2.6-6.1-4-9.8-4c-3.7,0-7.2,1.5-9.8,4 L256,197.8L124.9,68.3c-2.6-2.6-6.1-4-9.8-4c-3.7,0-7.2,1.5-9.8,4L68,105.9c-5.4,5.4-5.4,14.2,0,19.6l131.5,130L68.4,387.1 c-2.6,2.6-4.1,6.1-4.1,9.8c0,3.7,1.4,7.2,4.1,9.8l37.4,37.6c2.7,2.7,6.2,4.1,9.8,4.1c3.5,0,7.1-1.3,9.8-4.1L256,313.1l130.7,131.1 c2.7,2.7,6.2,4.1,9.8,4.1c3.5,0,7.1-1.3,9.8-4.1l37.4-37.6c2.6-2.6,4.1-6.1,4.1-9.8C447.7,393.2,446.2,389.7,443.6,387.1z"></path>
							</svg>
						</span>
					</button>
				</div>
				<div class="vue-lb-figure">
					<img class="vue-lb-modal-image" :src="state.selectedImgPath" @touchstart="touchStart"
						@touchend="touchEnd" @click="toggleFullScreen"
						ondragstart="return false"/>
					<div class="vue-lb-info" v-html="state.selectedCaption"></div>
					<div class="vue-lb-footer">
						<div class="vue-lb-footer-info"></div>
						<div class="vue-lb-footer-count">
							{{ state.selectedIndex + 1 }} / {{ images.length }}
						</div>
					</div>
				</div>
			</div>
			<div class="vue-lb-thumbnail-wrapper">
				<div v-if="state.hasThumb" class="vue-lb-thumbnail">
					<button type="button" title="Previous" class="vue-lb-thumbnail-arrow vue-lb-thumbnail-left"
							@click="prev">
						<span>
							<svg fill="white" x="0px" y="0px" width="100%" height="100%" viewBox="0 0 512 512">
								<path
									d="M213.7,256L213.7,256L213.7,256L380.9,81.9c4.2-4.3,4.1-11.4-0.2-15.8l-29.9-30.6c-4.3-4.4-11.3-4.5-15.5-0.2L131.1,247.9 c-2.2,2.2-3.2,5.2-3,8.1c-0.1,3,0.9,5.9,3,8.1l204.2,212.7c4.2,4.3,11.2,4.2,15.5-0.2l29.9-30.6c4.3-4.4,4.4-11.5,0.2-15.8 L213.7,256z"></path>
							</svg>
						</span>
					</button>
					<div v-for="(img, i) in images" :key="i" class="vue-lb-modal-thumbnail" lazy="loaded" :style="{
				'background-image': 'url(\'' + img.thumb + '\')',
				display:
				(i < 5 && state.selectedIndex < 3) ||
				(state.selectedIndex > images.length - 5 &&
					images.length - i <= 5 &&
					i <= state.selectedIndex + 2) ||
				(i >= state.selectedIndex - 2 && i <= state.selectedIndex + 2)
					? ''
					: 'none',
			}" @click="selected(i, img.src, img.caption)"></div>
					<button type="button" title="Next" class="vue-lb-thumbnail-arrow vue-lb-thumbnail-right" @click="next">
						<span>
							<svg fill="white" x="0px" y="0px" width="100%" height="100%" viewBox="0 0 512 512">
								<path
									d="M298.3,256L298.3,256L298.3,256L131.1,81.9c-4.2-4.3-4.1-11.4,0.2-15.8l29.9-30.6c4.3-4.4,11.3-4.5,15.5-0.2l204.2,212.7 c2.2,2.2,3.2,5.2,3,8.1c0.1,3-0.9,5.9-3,8.1L176.7,476.8c-4.2,4.3-11.2,4.2-15.5-0.2L131.3,446c-4.3-4.4-4.4-11.5-0.2-15.8 L298.3,256z"></path>
							</svg>
						</span>
					</button>
				</div>
			</div>
			<transition name="van-fade">
				<div v-show="state.showPageButton">
					<button type="button" title="Previous" class="vue-lb-arrow vue-lb-left"
							@click="prev">
						<span>
							<svg fill="white" x="0px" y="0px" width="100%" height="100%" viewBox="0 0 512 512">
								<path
									d="M213.7,256L213.7,256L213.7,256L380.9,81.9c4.2-4.3,4.1-11.4-0.2-15.8l-29.9-30.6c-4.3-4.4-11.3-4.5-15.5-0.2L131.1,247.9 c-2.2,2.2-3.2,5.2-3,8.1c-0.1,3,0.9,5.9,3,8.1l204.2,212.7c4.2,4.3,11.2,4.2,15.5-0.2l29.9-30.6c4.3-4.4,4.4-11.5,0.2-15.8 L213.7,256z"></path>
							</svg>
						</span>
					</button>
					<button type="button" title="Next" class="vue-lb-arrow vue-lb-right"
							@click="next">
						<span>
							<svg fill="white" x="0px" y="0px" width="100%" height="100%" viewBox="0 0 512 512"
								xml:space="preserve">
								<path
									d="M298.3,256L298.3,256L298.3,256L131.1,81.9c-4.2-4.3-4.1-11.4,0.2-15.8l29.9-30.6c4.3-4.4,11.3-4.5,15.5-0.2l204.2,212.7 c2.2,2.2,3.2,5.2,3,8.1c0.1,3-0.9,5.9-3,8.1L176.7,476.8c-4.2,4.3-11.2,4.2-15.5-0.2L131.3,446c-4.3-4.4-4.4-11.5-0.2-15.8 L298.3,256z"></path>
							</svg>
						</span>
					</button>
				</div>
			</transition>
		</div>
	</teleport>
</template>

<script>
// https://github.com/linmasahiro/vue3-image-box

import {defineComponent, reactive} from "vue";

export default defineComponent({
	name: "my-image-box",
	props: {
		imagesListClass: Object,
		containerStyle: Object,
		thumbStyle: Object,
		showImageList: {
			type: Boolean,
			default: true
		},
		images: {
			type: Array,
			default: () => {
				return [];
			},
		},
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	setup(props) {
		const state = reactive({
			show: false,
			isChanging: false,
			isFullScreen: false,
			selectedIndex: 1,
			hasThumb: props.images.some(img => img.thumb),
			showPageButton: false,
			selectedImgPath: "",
			selectedCaption: "",
		});
		const selected = (index, path, caption) => {
			state.selectedIndex = index;
			state.selectedImgPath = path;
			state.selectedCaption = caption;
			state.show = true;
		};
		const show = () => {
			state.hasThumb = props.images.some(img => img.thumb);
			state.selectedIndex = 0;
			state.selectedImgPath = props.images[0].src;
			state.selectedCaption = props.images[0].src;
			state.show = true;
		};
		const preShow = (promise) => {
			state.show = true;
			promise.then(() => {
				state.hasThumb = props.images.some(img => img.thumb);
				state.selectedIndex = 0;
				state.selectedImgPath = props.images[0].src;
				state.selectedCaption = props.images[0].src;
			});
		}
		const prev = () => {
			if (state.selectedIndex == 0) {
				return false;
			}
			let prevIndex = state.selectedIndex - 1;
			state.selectedIndex = prevIndex;
			state.selectedImgPath = props.images[prevIndex].src;
			state.selectedCaption = props.images[prevIndex].caption;
		};
		const next = () => {
			if (state.selectedIndex == props.images.length - 1) {
				return false;
			}
			let nextIndex = state.selectedIndex + 1;
			state.selectedIndex = nextIndex;
			state.selectedImgPath = props.images[nextIndex].src;
			state.selectedCaption = props.images[nextIndex].caption;
		};

		const touchState = reactive({
			startX: 0
		})
		const touchStart = (e) => {
			touchState.startX = e.changedTouches[0].clientX;
		}
		const touchEnd = (e) => {
			if (e.changedTouches[0].clientX > touchState.startX) {
				prev();
			} else {
				next();
			}
			touchState.startX = 0;
		}

		["", "webkit", "moz", "ms"].forEach(
			prefix => document.addEventListener(prefix + "fullscreenchange", (event) => {
				state.isFullScreen = document.fullscreenElement !== null;
			}, false)
		);

		function toggleFullScreen() {
			const doc = document;
			const el = this.$refs.fullScreenContainer;
			if (state.isFullScreen) {
				const exitFullScreen = doc.exitFullscreen || doc.cancelFullScreen || doc.webkitCancelFullScreen || doc.mozCancelFullScreen;
				exitFullScreen.call(doc);
				state.isFullScreen = false;
			} else {
				const fullScreen = el.requestFullscreen || el.webkitRequestFullScreen || el.mozRequestFullScreen || el.msRequestFullScreen;
				fullScreen && fullScreen.call(el);
				state.isFullScreen = true;
			}
		}

		function mouseMove(e) {
			const containerWidth = this.$refs.container.clientWidth;
			const offset = e.clientX - this.$refs.container.offsetLeft;
			const diff = 80;
			if (offset < diff || offset > containerWidth - diff) {
				this.state.showPageButton = true;
			} else {
				this.state.showPageButton = false;
			}
		}

		return {
			state,
			selected,
			toggleFullScreen,
			show,
			preShow,
			prev,
			next,
			touchStart,
			touchEnd,
			mouseMove
		};
	},
});
</script>

<style scoped>
.vue-lb-box {
	width: 100%;
}

.vue-lb-container {
	align-items: center;
	background-color: rgba(0, 0, 0, 0.5);
	backdrop-filter: blur(20px);
	box-sizing: border-box;
	display: -ms-flexbox;
	display: flex;
	height: 100%;
	justify-content: center;
	left: 0;
	padding: 10px;
	position: fixed;
	top: 0;
	width: 100%;
	z-index: 2500;
	-webkit-align-items: center;
	-moz-box-sizing: border-box;
	-webkit-justify-content: center;
	-ms-flex-align: center;
	-webkit-box-align: center;
	-ms-flex-pack: center;
	-webkit-box-pack: center;
	touch-action: pan-y;
	user-select: none;
	-webkit-user-drag: none;
	-webkit-tap-highlight-color: rgba(0, 0, 0, 0);
}

.vue-lb-content {
	margin-bottom: 60px;
	max-width: 1024px;
	position: relative;
}

.vue-lb-header {
	display: -ms-flexbox;
	display: flex;
	justify-content: space-between;
	height: 40px;
	-webkit-justify-content: space-between;
	-ms-flex-pack: justify;
	-webkit-box-pack: justify;
}

.vue-lb-button-close {
	background: none;
	border: none;
	cursor: pointer;
	outline: none;
	position: fixed;
	top: 10px;
	right: 35px;
	height: 40px;
	width: 40px;
}

.vue-lb-figure {
	margin: 0;
	display: block;
	position: relative;
}

img.vue-lb-modal-image {
	max-height: calc(100vh - 140px);
	box-shadow: 0 0 1.5rem rgba(0, 0, 0, 0.45);
	cursor: pointer;
	display: block;
	height: auto;
	margin: 0 auto;
	max-width: 100%;
	user-select: none;
	-webkit-user-select: none;
	-moz-user-select: none;
	-ms-user-select: none;
}

.vue-lb-info {
	visibility: initial;
	position: absolute;
	bottom: 25px;
	color: #fff;
	background-color: rgba(0, 0, 0, 0.7);
	height: 40px;
	width: 100%;
	font-family: Helvetica Neue, Helvetica, Arial, sans-serif;
	text-align: center;
}

.vue-lb-footer {
	box-sizing: border-box;
	color: #fff;
	cursor: auto;
	display: -ms-flexbox;
	display: flex;
	justify-content: space-between;
	left: 0;
	line-height: 1.3;
	padding: 5px 0;
	-moz-box-sizing: border-box;
	-webkit-justify-content: space-between;
	-ms-flex-pack: justify;
	-webkit-box-pack: justify;
}

.vue-lb-footer-info {
	display: block;
	flex: 1 1 0;
	-webkit-flex: 1 1 0;
	-ms-flex: 1 1 0;
}

.vue-lb-footer-count {
	color: hsla(0, 0%, 100%, 0.75);
	font-size: 0.85em;
	padding-left: 1em;
}

.vue-lb-thumbnail {
	bottom: 10px;
	height: 50px;
	padding: 0 50px;
	text-align: center;
	white-space: nowrap;
	display: inline-block;
	position: relative;
}

.vue-lb-modal-thumbnail {
	box-shadow: inset 0 0 0 1px hsla(0, 0%, 100%, 0.2);
}

.vue-lb-modal-thumbnail,
.vue-lb-modal-thumbnail-active {
	background-position: 50%;
	background-size: cover;
	border-radius: 2px;
	cursor: pointer;
	display: inline-block;
	height: 50px;
	margin: 2px;
	overflow: hidden;
	width: 50px;
}

.vue-lb-modal-thumbnail-active {
	box-shadow: inset 0 0 0 2px #fff;
}

.vue-lb-thumbnail-arrow {
	height: 54px;
	width: 40px;
	background: none;
	border: none;
	border-radius: 4px;
	cursor: pointer;
	outline: none;
	padding: 10px;
	position: absolute;
	top: 50%;
	-webkit-touch-callout: none;
	user-select: none;
	height: 50px;
	margin-top: -25px;
	width: 30px;
	-webkit-user-select: none;
	-moz-user-select: none;
	-ms-user-select: none;
}

.vue-lb-thumbnail-left {
	left: 10px;
}

.vue-lb-thumbnail-right {
	right: 10px;
}

.vue-lb-arrow {
	background: none;
	border: none;
	border-radius: 4px;
	cursor: pointer;
	outline: none;
	padding: 10px;
	position: absolute;
	top: 50%;
	-webkit-touch-callout: none;
	user-select: none;
	height: 120px;
	margin-top: -60px;
	width: 40px;
	-webkit-user-select: none;
	-moz-user-select: none;
	-ms-user-select: none;
}

.vue-lb-left {
	left: 10px;
}

.vue-lb-right {
	right: 10px;
}

.vue-lb-open {
	overflow: hidden;
}

.vue-lb-thumbnail-wrapper {
	bottom: 10px;
	height: 50px;
	left: 0;
	margin: 0 auto;
	position: absolute;
	right: 0;
	text-align: center;
	top: auto;
}

@media (min-width: 500px) {
	.vue-lb-thumbnail-arrow {
		width: 40px;
	}
}

@media (min-width: 768px) {
	.vue-lb-arrow {
		width: 70px;
	}
}
</style>