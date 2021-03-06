<template>
	<Icon :component="RiHome3LineSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiHome3LineSvg from "@/assets/icons/svg/RiHome3LineSvg";

export default {
	name: 'RiHome3Line',
	components: {
		Icon
	},
	data() {
		return {
			RiHome3LineSvg: markRaw(RiHome3LineSvg),
		}
	}
}
</script>

<style scoped>

</style>