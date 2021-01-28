<template>
	<div class="mine">
		<a-row style="padding-top: 2rem;">
			<a-col span="5" offset="2">
				<img class="ava" :src="myAvatarUrl"/>
				<a-upload-m
					v-model:fileList="fileList"
					accept="image/*"
					list-type="picture-card"
					class="avatar-uploader"
					:show-upload-list="false"
					:action="uploadUrl"
					:before-upload="beforeUpload"
					@change="handleChange">
					<img v-if="imageUrl" :src="imageUrl" alt="avatar"/>
					<div v-else>
						<div v-if="loading">Loading</div>
						<div class="ant-upload-text">Upload</div>
					</div>
				</a-upload-m>
			</a-col>
			<a-col span="16" offset="1">
				<div>
					<p v-if="hasSigned" class="name">{{ me.user.nick_name }}</p>
					<a-button v-else type="link" class="name" @click="signIn">点击登录</a-button>
				</div>
			</a-col>
		</a-row>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa';
import global from '@/lib/js/global-state';
import {message} from 'ant-design-vue';
import AUploadM from '@/components/upload/Upload';

function getBase64(img, callback) {
	const reader = new FileReader();
	reader.addEventListener('load', () => callback(reader.result));
	reader.readAsDataURL(img);
}

export default {
	name: 'Mine',
	components: {
		AUploadM
	},
	data() {
		this.uploadUrl = lpt.baseUrl + '/oss/ava';
		return {
			me: global.curOperator,
			fileList: [],
			loading: false,
			imageUrl: ''
		}
	},
	computed: {
		hasSigned() {
			return global.hasSigned.value;
		},
		myAvatarUrl() {
			return lpt.getUserAvatarUrl(this.me.user);
		}
	},
	methods: {
		signIn() {
			this.$router.push({name: 'signIn'});
		},
		handleChange(info) {
			if (info.file.status === 'uploading') {
				this.loading = true;
				return;
			}
			if (info.file.status === 'done') {
				// Get this url from response in real world.
				getBase64(info.file.originFileObj, imageUrl => {
					this.imageUrl = imageUrl;
					this.loading = false;
				});
			}
			if (info.file.status === 'error') {
				this.loading = false;
			}
		},
		beforeUpload(file) {
			const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
			if (!isJpgOrPng) {
				message.error('You can only upload JPG file!');
			}
			const isLt2M = file.size / 1024 / 1024 < 2;
			if (!isLt2M) {
				message.error('Image must smaller than 2MB!');
			}
			return isJpgOrPng && isLt2M;
		}
	}
}
</script>

<style scoped>
.mine {
	height: 100%;
}

.ava {
	border-radius: 100%;
	margin: 0.5rem 1rem;
	width: 3.5rem;
	height: 3.5rem;
}

.name {
	margin-top: 1rem;
	font-size: 1.5rem;
}

.avatar-uploader > .ant-upload {
	width: 128px;
	height: 128px;
}

.ant-upload-select-picture-card i {
	font-size: 32px;
	color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
	margin-top: 8px;
	color: #666;
}
</style>