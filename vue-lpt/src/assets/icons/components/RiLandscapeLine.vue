<template>
	<Icon :component="RiLandscapeLineSvg"/>
</template>

<script>
import { markRaw } from 'vue'
import Icon from '@ant-design/icons-vue'
import RiLandscapeLineSvg from '@/assets/icons/svg/RiLandscapeLineSvg';

export default {
	name: 'RiLandscapeLine',
	components: {
		Icon
	},
	data() {
		return {
			RiLandscapeLineSvg: markRaw(RiLandscapeLineSvg),
		}
	}
}
</script>

<style scoped>

</style>