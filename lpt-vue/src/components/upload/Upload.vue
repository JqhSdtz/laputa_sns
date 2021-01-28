<script>
import {Upload, Button} from 'ant-design-vue';
import {getSlot} from 'ant-design-vue/lib/_util/props-util';

export default {
	name: 'AUploadM',
	props: {
		...Upload.props
	},
	data() {
		return {
			showCapturePanel: false
		}
	},
	mounted() {
	},
	methods: {
		handleClick(event) {
			this.showCapturePanel = !this.showCapturePanel;
			event.stopPropagation();
		},
		getInputRef() {
			const aUpload = this.$refs.aUpload;
			const uploadRef = aUpload && aUpload.$refs.uploadRef;
			const uploaderRef = uploadRef && uploadRef.$refs.uploaderRef;
			return uploaderRef && uploaderRef.$refs.fileInputRef;
		},
		chooseCamera() {
			const input = this.getInputRef();
			if (!input) return;
			input.setAttribute('capture', 'camera');
			input.click();
		},
		chooseGallery() {
			const input = this.getInputRef();
			input.removeAttribute('capture');
			if (!input) return;
			input.click();
		}
	},
	render() {
		const componentProps = {
			...this.$props,
			ref: 'aUpload',
			...this.$attrs,
			openFileDialogOnClick: false,
			withCredentials: true
		};
		// 这里必须要在作用域内声明一个AUpload才能用作JSX
		// 不能直接写<Upload...
		const AUpload = Upload;
		const AButton = Button;

		const panelButtonStyle = {
			width: '100%',
			height: '3rem',
			borderBottom: 'none'
		};
		const panelStyle = {
			position: 'fixed',
			zIndex: 1,
			left: 0,
			bottom: 0,
			width: '100%'
		}
		const capturePanel = (
			// <div style="height: 500px;background-color:black">aaaaaaaaaa</div>
			<div style={panelStyle} v-show={this.showCapturePanel}>
				<AButton style={panelButtonStyle} onClick={this.chooseCamera}>
					拍照
				</AButton>
				<AButton style={panelButtonStyle} onClick={this.chooseGallery}>
					相册
				</AButton>
				<AButton style={panelButtonStyle} onClick={this.handleClick}>
					取消
				</AButton>
			</div>
		);
		// 必须有一个根节点，如果加别的，就再套一个div
		return (
			<div onClick={this.handleClick}>
				<AUpload {...componentProps}>{getSlot(this)}</AUpload>
				{capturePanel}
			</div>
		)
	}
}
</script>

<style scoped>

</style>