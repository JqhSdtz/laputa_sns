<template>
	<Icon :component="RiAppsFillSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiAppsFillSvg from "@/assets/icons/svg/RiAppsFillSvg";

export default {
	name: 'RiAppsFill',
	components: {
		Icon
	},
	data() {
		return {
			RiAppsFillSvg: markRaw(RiAppsFillSvg),
		}
	}
}
</script>

<style scoped>

</style>