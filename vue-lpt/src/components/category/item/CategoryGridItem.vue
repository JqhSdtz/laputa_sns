<template>
	<div style="text-align: center; position: relative">
		<img style="border-radius: 0.5rem;" :style="{width: size, height: size}" :src="iconImgUrl"
		     @click="(clickImg || showCategoryDetail)()"/>
		<p :class="{'link-name': isLinkName}" :title="linkTitle" v-clamp="2"
		   style="white-space: break-spaces;"
		   @click="(clickName || showCategoryDetail)()">
			{{ category.name }}
		</p>
		<slot/>
	</div>
</template>

<script>
import global from '@/lib/js/global';
import lpt from "@/lib/js/laputa/laputa";

export default {
	name: 'CategoryGridItem',
	props: {
		size: {
			type: String,
			default: '5.5rem'
		},
		categoryId: Number,
		isLinkName: Boolean,
		linkTitle: String,
		clickImg: Function,
		clickName: Function
	},
	data() {
		const category = global.states.categoryManager.get({
			itemId: this.categoryId
		});
		return {
			category
		};
	},
	computed: {
		iconImgUrl() {
			return lpt.getCategoryIconUrl(this.category);
		}
	},
	methods: {
		showCategoryDetail() {
			this.$router.push({
				path: '/category_detail/' + this.categoryId
			});
		}
	}
}
</script>

<style scoped>
.link-name {
	cursor: pointer;
}

.link-name:hover {
	text-decoration: underline;
}
</style>