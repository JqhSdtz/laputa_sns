<template>
	<post-list keep-scroll-top class='post-list'></post-list>
</template>

<script>
import PostList from '@/components/post/PostList'

export default {
	name: 'Index',
	components: {
		PostList
	}
}
</script>

<style scoped>
	.post-list {
		padding: 8px 0;
	}
</style>