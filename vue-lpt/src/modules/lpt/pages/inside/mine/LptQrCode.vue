<template>
	<div style="font-size: 1rem;text-align: center;margin-top:10%">
		<p>社区为移动端设计，扫码即可访问</p>
		<a href="https://lpt.jqh.zone" target="_blank">或点击链接打开</a>
	</div>
	<img id="qr-code" :src="lptQrCodeImg"/>
</template>

<script>
import lptQrCodeImg from '@/assets/qrcode/lpt_qr_code.png'

export default {
	name: 'LptQrCode',
	data() {
		return {
			lptQrCodeImg
		}
	}
}
</script>

<style scoped>
#qr-code {
	width: 60%;
	margin-left: 20%;
	margin-top: 2rem;
}

a:hover {
	text-decoration: underline;
}
</style>