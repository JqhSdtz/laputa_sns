<template>
	<Icon :component="RiUser3LineSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiUser3LineSvg from "@/assets/icons/svg/RiUser3LineSvg";

export default {
	name: 'RiUser3Line',
	components: {
		Icon
	},
	data() {
		return {
			RiUser3LineSvg: markRaw(RiUser3LineSvg),
		}
	}
}
</script>

<style scoped>

</style>