<template>
	<Icon :component="RiHome3FillSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiHome3FillSvg from "@/assets/icons/svg/RiHome3FillSvg";

export default {
	name: 'RiHome3Fill',
	components: {
		Icon
	},
	data() {
		return {
			RiHome3FillSvg: markRaw(RiHome3FillSvg),
		}
	}
}
</script>

<style scoped>

</style>