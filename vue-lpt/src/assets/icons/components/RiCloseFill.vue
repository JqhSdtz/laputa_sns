<template>
	<Icon :component="RiCloseFillSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiCloseFillSvg from '@/assets/icons/svg/RiCloseFillSvg';

export default {
	name: 'RiCloseFill',
	components: {
		Icon
	},
	data() {
		return {
			RiCloseFillSvg: markRaw(RiCloseFillSvg),
		}
	}
}
</script>

<style scoped>

</style>