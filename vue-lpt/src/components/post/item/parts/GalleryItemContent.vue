<template>
	<div style="font-size: 0.9rem">
        <div>
            <span>描述：</span><span>{{ galleryItem.desc }}</span>
        </div>
        <div>
            <span style="vertical-align: middle">封面：</span>
            <div v-if="env === 'lpt'" style="display: inline-block; vertical-align: middle">
                <a @click="showSingleImg(galleryItem.coverUrl)">
                    <van-image :src="galleryItem.coverUrl" fit="cover" width="50" height="50"/>
                </a>
            </div>
            <div v-if="env === 'blog'" style="display: inline-block; vertical-align: middle">
                <image-box 
                    :images="[{src: galleryItem.coverUrl, thumb: galleryItem.coverUrl}]"
                    :thumb-style="{height: '50px', width: '50px'}"/>
            </div>
        </div>
        <div>
            <span>张数：</span><span>{{ galleryItem.photoNum }}</span>
        </div>
    </div>
</template>

<script>
import {ImagePreview} from 'vant';
import global from '@/lib/js/global';
import ImageBox from '@/components/global/ImageBox';

export default {
	name: 'GalleryItemContent',
	props: {
		galleryItem: Object
	},
    components: {
        ImageBox
    },
    data() {
        return {
            env: global.vars.env,
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