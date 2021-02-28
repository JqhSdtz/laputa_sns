<template>
	<div>{{ statData }}</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';

export default {
	name: 'DruidStat',
	data() {
		return {
			statData: ''
		}
	},
	created() {
		lpt.ajax({
			url: lpt.baseUrl + '/druid/stat',
			success: (data) => {
				console.log(data);
				this.statData = JSON.stringify(data.object);
			}
		});
	}
}
</script>

<style scoped>

</style>