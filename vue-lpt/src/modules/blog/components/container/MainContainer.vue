<template>
	<slot></slot>
</template>

<script>
export default {
	name: 'MainContainer',
	provide: {
		lptContainer: 'blogMain'
	}
}
</script>

<style scoped>

</style>