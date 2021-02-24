<template>
	<van-dialog v-model:show="showDialog" :title="curTitle" :before-close="beforeClose" show-cancel-button>
		<slot>
			<slot name="tip">
				<p v-if="curTipMessage !== ''">{{ curTipMessage }}</p>
			</slot>
			<a-form-item ref="formItemRef" style="width: 90%;margin-left: 5%;">
				<a-textarea v-model:value="value" :placeholder="curPlaceHolder"
				            @change="onInputChange" @focus="onInputFocus" @blur="onInputBlur" auto-size id="text-area"/>
			</a-form-item>
		</slot>
	</van-dialog>
</template>
<script>
import {clearError, makeError} from '@/lib/js/uitls/form-util';
import global from '@/lib/js/global';

export default {
	name: 'PromptDialog',
	props: {
		title: {
			type: String,
			default: '输入操作原因'
		},
		tipMessage: {
			type: String,
			default: ''
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
		return {
			curTitle: this.title,
			curTipMessage: this.tipMessage,
			curPlaceHolder: this.placeholder,
			curOnValidate: this.onValidate,
			curErrorMessage: this.errorMessage,
			showDialog: false,
			oriValue: '',
			value: ''
		}
	},
	created() {
		this.promptSymbol = Symbol();
	},
	methods: {
		prompt(param) {
			this.curTitle = param.title || this.title;
			this.focusTitle = param.focusTitle;
			this.curTipMessage = param.tipMessage || this.tipMessage;
			this.curPlaceHolder = param.placeholder || this.placeholder;
			this.curOnValidate = param.onValidate || this.onValidate;
			this.curErrorMessage = param.errorMessage || this.errorMessage;
			this.oriValue = param.value;
			this.value = param.value || this.value;
			this.onConfirm = param.onConfirm;
			this.onCancel = param.onCancel;
			this.onFinish = param.onFinish;
			this.showDialog = true;
			return this.promptSymbol;
		},
		beforeClose(action) {
			const value = this.value;
			const clearDialog = () => {
				this.curTipMessage = '';
				this.curPlaceHolder = '';
				this.oriValue = '';
				this.value = '';
			};
			const closeDialog = () => {
				clearDialog();
				this.showDialog = false;
			};
			const inputRef = this.$refs.formItemRef;
			if (action === 'cancel') {
				this.onCancel && this.onCancel(value);
				this.onFinish && this.onFinish(value);
				clearError(inputRef);
				closeDialog();
				return;
			}
			if (this.oriValue !== '' && this.oriValue === this.value) {
				// 没做改动，点击确定时直接关闭
				closeDialog();
				return;
			}
			const afterConfirm = () => {
				clearError(inputRef);
				clearDialog();
				const res = this.onConfirm && this.onConfirm(value);
				this.$emit('confirm', value);
				if (typeof res === 'undefined') {
					this.onFinish && this.onFinish(value);
					this.showDialog = false;
				} else if (res instanceof Promise) {
					res.then(() => {
						this.onFinish && this.onFinish(value);
						this.showDialog = false;
					});
				} else if (res === this.promptSymbol) {
					return false;
				} else {
					res && this.onFinish && this.onFinish(value);
					this.showDialog = !res;
				}
			};
			const ret = this.curOnValidate(value);
			const isRetString = typeof ret === 'string' || ret instanceof String;
			if (!ret) {
				makeError(inputRef, this.curErrorMessage);
			} else if (isRetString) {
				makeError(inputRef, ret);
			} else if (ret instanceof Promise) {
				ret.then(() => {
					afterConfirm();
				}).catch(errMsg => makeError(inputRef, errMsg));
				return false;
			} else {
				afterConfirm();
			}
		},
		onInputChange() {
			const value = this.value;
			const inputRef = this.$refs.formItemRef;
			if (value.length >= 5 && value.length <= 256) {
				clearError(inputRef);
			}
		},
		onInputFocus() {
			if (this.focusTitle) {
				this.oriTitle = this.curTitle;
				this.curTitle = this.focusTitle
			}
		},
		onInputBlur() {
			if (this.oriTitle) {
				this.curTitle = this.oriTitle;
			}
		}
	}
}
</script>

<style scoped>
#text-area {
	border-top: none;
	border-left: none;
	border-right: none;
	margin-top: 1rem;
}
</style>