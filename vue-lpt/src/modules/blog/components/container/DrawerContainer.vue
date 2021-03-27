<template>
	<slot></slot>
</template>

<script>
export default {
	name: 'DrawerContainer',
	provide: {
		lptContainer: 'blogDrawer'
	}
}
</script>

<style scoped>

</style>