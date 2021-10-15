<script>
import {Upload} from 'ant-design-vue';
import {ActionSheet} from 'vant';
import {getSlot} from 'ant-design-vue/lib/_util/props-util';

export default {
	name: 'AUploadM',
	props: {
		...Upload.props,
		width: {
			type: String,
			default: '100%'
		}
	},
	data() {
		return {
			showCapturePanel: false
		}
	},
	methods: {
		handleClick(event) {
			if (event.target.className === 'a-upload-m') {
				// 加上遮罩之后，点击整个屏幕都会触发点击事件
				// 需要过滤一下
				this.showCapturePanel = true;
				event.stopPropagation();
			}
		},
		getInputRef() {
			const aUpload = this.$refs.aUpload;
			const uploadRef = aUpload && aUpload.$refs.uploadRef;
			const uploaderRef = uploadRef && uploadRef.$refs.uploaderRef;
			return uploaderRef && uploaderRef.$refs.fileInputRef;
		},
		chooseMethod(itemId) {
			const input = this.getInputRef();
			if (!input) return;
			if (itemId === 'camera') {
				input.setAttribute('capture', 'camera');
			} else if (itemId === 'gallery') {
				input.removeAttribute('capture');
			}
			input.click();
		}
	},
	render() {
		const ref = this;
		const componentProps = {
			...this.$props,
			ref: 'aUpload',
			...this.$attrs,
			openFileDialogOnClick: false,
			withCredentials: true
		};
		function onSelect(item) {
			// 默认情况下点击选项时不会自动收起
			ref.chooseMethod(item.id);
		}
		const actionSheetProp = {
			ref: 'actionSheet',
			actions: [
				{
					id: 'camera',
					name: '拍照'
				}, {
					id: 'gallery',
					name: '相册'
				}
			],
			style: {
				width: this.width
			},
			class: {
				'width-transition': true
			},
			cancelText: "取消",
			description: '请选择上传方式',
			closeOnClickAction: true,
			onSelect: onSelect
		}
		// 这里必须要在作用域内声明一个VanActionSheet才能用作JSX
		// 不能直接写<ActionSheet...
		const VanActionSheet = ActionSheet;
		// 必须有一个根节点，如果加别的，就再套一个div
		return (
			<div className="a-upload-m" onClick={this.handleClick}>
				<AUpload {...componentProps}>{getSlot(this)}</AUpload>
				<VanActionSheet v-model={[this.showCapturePanel, 'show']} {...actionSheetProp} />
			</div>
		)
	}
}
</script>

<style scoped>

</style>