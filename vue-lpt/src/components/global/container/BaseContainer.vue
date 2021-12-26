<template>
	<prompt-wrapper>
		<slot></slot>
	</prompt-wrapper>
</template>

<script>
import PromptWrapper from '../wrapper/PromptWrapper'

export default {
	name: 'BaseContainer',
	props: {
		name: String
	},
	components: {
		PromptWrapper
	},
	provide() {
		return {
			lptContainer: this.name
		}
	},
	created() {

	}
}
</script>

<style scoped>

</style>