<template>
	<van-dialog v-model:show="showDialog" :title="curTitle" :before-close="beforeClose" show-cancel-button>
		<a-form-item ref="formItemRef">
			<a-textarea v-model:value="value" :placeholder="curPlaceHolder"
			            @change="onInputChange" auto-size/>
		</a-form-item>
	</van-dialog>
</template>
<script>
import {clearError, makeError} from '@/lib/js/uitls/form-util';
import global from '@/lib/js/global';

export default {
	name: 'GlobalPromptDialog',
	props: {
		title: {
			type: String,
			default: '输入操作原因'
		},
		placeholder: {
			type: String,
			default: '请输入操作原因(5-256个字)'
		},
		onValidate: {
			type: Function,
			default(value) {
				return value.length >= 5 && value.length <= 256
			}
		},
		errorMessage: {
			type: String,
			default: '输入内容长度应在5-256个字之间'
		}
	},
	emits: ['confirm'],
	data() {
		const ref = this;
		return {
			curTitle: this.title,
			curPlaceHolder: this.placeholder,
			curOnValidate: this.onValidate,
			curErrorMessage: this.errorMessage,
			showDialog: false,
			value: '',
			beforeClose(action) {
				const inputRef = ref.$refs.formItemRef;
				if (action === 'cancel') {
					clearError(inputRef);
					ref.value = '';
					ref.showDialog = false;
					return;
				}
				const value = ref.value;
				const ret = ref.curOnValidate(value);
				const isRetString = typeof ret === 'string' || ret instanceof String;
				if (!ret) {
					makeError(inputRef, ref.curErrorMessage);
				} else if (isRetString) {
					makeError(inputRef, ret);
				} else {
					clearError(inputRef);
					ref.onConfirm && ref.onConfirm(value);
					ref.$emit('confirm', value);
					ref.value = '';
					ref.showDialog = false;
				}
			},
			onInputChange() {
				const value = ref.value;
				const inputRef = ref.$refs.formItemRef;
				if (value.length >= 5 && value.length <= 256) {
					clearError(inputRef);
				}
			}
		}
	},
	created() {
		global.methods.prompt = (param) => {
			this.curTitle = param.title || this.title;
			this.curPlaceHolder = param.placeholder || this.placeholder;
			this.curOnValidate = param.onValidate || this.onValidate;
			this.curErrorMessage = param.errorMessage || this.errorMessage;
			this.onConfirm = param.onConfirm;
			this.showDialog = true;
		}
	}
}
</script>

<style scoped>

</style>