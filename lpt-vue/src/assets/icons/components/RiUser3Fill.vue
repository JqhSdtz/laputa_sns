<template>
	<Icon :component="RiUser3FillSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiUser3FillSvg from "@/assets/icons/svg/RiUser3FillSvg";

export default {
	name: 'RiUser3Fill',
	components: {
		Icon
	},
	data() {
		return {
			RiUser3FillSvg: markRaw(RiUser3FillSvg),
		}
	}
}
</script>

<style scoped>

</style>