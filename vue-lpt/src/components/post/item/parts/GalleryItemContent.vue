<template>
	<div style="font-size: 0.9rem">
        <div>
            <span>描述：</span><span>{{ galleryItem.desc }}</span>
        </div>
        <div>
            <span style="vertical-align: middle">封面：</span>
            <div v-if="lptContainer === 'lptMain'" style="display: inline-block; vertical-align: middle">
                <a @click="showSingleImg(galleryItem.coverUrl)">
                    <van-image :src="galleryItem.coverUrl" fit="cover" width="4rem" height="4rem"/>
                </a>
            </div>
            <div v-if="lptContainer === 'blogMain' || lptContainer === 'blogDrawer'" style="display: inline-block; vertical-align: middle">
                <image-box 
                    :images="[{src: galleryItem.coverUrl, thumb: galleryItem.coverUrl}]"
                    :thumb-style="{height: '4rem', width: '4rem'}"/>
            </div>
        </div>
        <div>
            <span>张数：</span><span>{{ galleryItem.photoNum }}</span>
        </div>
    </div>
</template>

<script>
import {ImagePreview} from 'vant';
import ImageBox from '@/components/global/ImageBox';

export default {
	name: 'GalleryItemContent',
	props: {
		galleryItem: Object
	},
    components: {
        ImageBox
    },
	inject: {
		lptContainer: {
			type: String
		}
	},
    methods: {
        showSingleImg(url) {
			ImagePreview({
				images: [url],
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

</style>