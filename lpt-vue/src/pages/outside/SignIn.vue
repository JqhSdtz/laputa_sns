<template>
	<div>This is sign in page!</div>
	<a-button @click="backToHome">返回</a-button>
</template>

<script>
export default {
	name: 'SignIn',
	inject: {
		backToHome: {
			type: Function
		}
	}
}
</script>

<style scoped>

</style>