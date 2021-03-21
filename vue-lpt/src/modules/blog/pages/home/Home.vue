<template>
	<div style="height: 100%; width: 100%; background-size: cover; background-position: bottom"
	     :style="{backgroundImage: `url(${coverUrl})`}">
	</div>
</template>

<script>
import axios from 'axios';

export default {
	name: 'Home',
	data() {
		return {
			coverUrl: ''
		}
	},
	created() {
		axios.get('/static/blog/description.json').then((result) => {
			const description = result.data;
			this.coverUrl = description.cover.url;
		});
	}
}
</script>

<style scoped>

</style>