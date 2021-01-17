<template>
	<Home/>
</template>

<script>
import Home from './components/home/Home'

export default {
	name: 'App',
	components: {
		Home
	}
}
</script>

<style>
#app {
	height: 100%;
	font-family: Avenir, Helvetica, Arial, sans-serif;
	-webkit-font-smoothing: antialiased;
	-moz-osx-font-smoothing: grayscale;
	text-align: center;
	color: #2c3e50;
}

body, html {
	width: 100%;
	height: 100%;
}

body {
	 margin: 0;
}
</style>
