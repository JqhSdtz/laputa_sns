<template>
	<Icon :component="RiDashboardFillSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiDashboardFillSvg from "@/assets/icons/svg/RiDashboardFillSvg";

export default {
	name: 'RiDashboardFill',
	components: {
		Icon
	},
	data() {
		return {
			RiDashboardFillSvg: markRaw(RiDashboardFillSvg),
		}
	}
}
</script>

<style scoped>

</style>