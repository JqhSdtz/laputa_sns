<template>
	<Icon :component="RiStackLineSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiStackLineSvg from '@/assets/icons/svg/RiStackLineSvg';

export default {
	name: 'RiStackLine',
	components: {
		Icon
	},
	data() {
		return {
			RiStackLineSvg: markRaw(RiStackLineSvg),
		}
	}
}
</script>

<style scoped>

</style>