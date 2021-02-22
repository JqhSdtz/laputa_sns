<template>
	<div @click.stop="showCategoryDetail" style="text-align: center; position: relative">
		<img style="border-radius: 0.5rem;" :style="{width: size, height: size}" :src="iconImgUrl"/>
		<p>{{category.name}}</p>
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
	},
	data() {
		const category = global.states.categoryManager.get(this.categoryId);
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
				name: 'categoryDetail',
				params: {
					categoryId: this.categoryId
				}
			});
		}
	}
}
</script>

<style scoped>

</style>