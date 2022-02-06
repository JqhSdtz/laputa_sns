<template>
	<div ref="imageListContainer">
		<a-row v-if="lptContainer === 'lptMain'">
			<a-col :span="8" style="display: inline;" v-for="(img, idx) in imgUrlList" :key="img">
				<van-image :src="img" fit="cover"
				           :width="imgSize"
				           :height="imgSize"
				           style="margin: 0.5rem"
				           @click="showImgPreview(idx)"/>
			</a-col>
		</a-row>
		<div v-if="lptContainer === 'blogMain' || lptContainer === 'blogDrawer'" class="image-box-list">
			<image-box :images="imageBoxList"
			           :thumbStyle="{height: imgSize + 'px', width: imgSize + 'px', margin: '0.5rem'}"/>
		</div>
	</div>
</template>

<script>
import ImageBox from '@/components/global/ImageBox';
import {ImagePreview} from 'vant';
import remHelper from '@/lib/js/uitls/rem-helper';

export default {
	name: 'PostImageList',
	props: {
		imgUrlList: Array,
		fullUrlList: Array,
		imageBoxList: Array
	},
	components: {
		ImageBox
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	data() {
		return {
			imgSize: 75
		}
	},
	mounted() {
		this.$nextTick(() => this.setPostImageSize());
		window.addEventListener('resize', this.setPostImageSize);
	},
	methods: {
		setPostImageSize() {
			const width = this.$refs.imageListContainer.clientWidth;
			if (this.lptContainer === 'blogMain') {
				this.imgSize = width / 8 - remHelper.remToPx(1);
			} else {
				this.imgSize = width / 3 - remHelper.remToPx(1);
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
	}
}
</script>

<style scoped>
:global(.image-box-list img) {
	display: inline;
}

:global(.image-box-list>div>div) {
	display: inline-block;
}
</style>