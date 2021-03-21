<template>
	<json-viewer :value="statData" expanded></json-viewer>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';

export default {
	name: 'DruidStat',
	data() {
		return {
			statData: []
		}
	},
	created() {
		lpt.ajax({
			url: lpt.baseApiUrl + '/druid/stat',
			success: (result) => {
				this.statData = result.object;
			}
		});
	}
}
</script>

<style scoped>

</style>