<template>
	<div class="content-area">
		<p class="title">{{ comment.title }}</p>
<!--		<pre class="content">{{ comment.content }}</pre>-->
		<ellipsis class="content" :content="comment.content" :rows="5"/>
	</div>
</template>

<script>
import Ellipsis from '@/components/global/Ellipsis';

export default {
	name: 'ContentArea',
	components: {
		Ellipsis
	},
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
	font-size: 0.9rem;
	white-space: pre-wrap;
}

.title {
	text-align: center;
	font-weight: bold;
	word-wrap: break-word;
	word-break: normal;
}
</style>