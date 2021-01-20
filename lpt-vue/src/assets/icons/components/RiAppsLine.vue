<template>
	<Icon :component="RiAppsLineSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiAppsLineSvg from "@/assets/icons/svg/RiAppsLineSvg";

export default {
	name: 'RiAppsLine',
	components: {
		Icon
	},
	data() {
		return {
			RiAppsLineSvg: markRaw(RiAppsLineSvg),
		}
	}
}
</script>

<style scoped>

</style>