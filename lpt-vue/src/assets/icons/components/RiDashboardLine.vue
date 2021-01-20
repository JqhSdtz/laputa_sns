<template>
	<Icon :component="RiDashboardLineSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiDashboardLineSvg from "@/assets/icons/svg/RiDashboardLineSvg";

export default {
	name: 'RiDashboardLine',
	components: {
		Icon
	},
	data() {
		return {
			RiDashboardLineSvg: markRaw(RiDashboardLineSvg),
		}
	}
}
</script>

<style scoped>

</style>