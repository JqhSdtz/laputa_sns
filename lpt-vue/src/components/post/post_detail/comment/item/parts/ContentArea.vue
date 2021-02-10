<template>
	<div class="content-area">
		<p class="title">{{ comment.title }}</p>
		<pre class="content">{{ comment.content }}</pre>
	</div>
</template>

<script>
export default {
	name: 'ContentArea',
	props: {
		comment: Object
	}
}
</script>

<style scoped>
.content {
	text-align: left;
	word-break: break-word;
	padding: 0 1rem;
	white-space: pre-wrap;
}

.title {
	text-align: center;
	font-weight: bold;
	word-wrap: break-word;
	word-break: normal;
}
</style>