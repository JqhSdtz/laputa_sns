<template>
	<van-dialog class="prompt-dialog" v-model:show="showDialog" :title="curTitle" :before-close="beforeClose" 
		:teleport="teleport" show-cancel-button>
		<slot>
			<p v-if="curTipMessage !== ''" 
				style="text-align: center; margin-top: 0.5rem"
				:style="{marginTop: curInputType === 'none' ? '0.5rem' : '0'}">
				{{ curTipMessage }}
			</p>
			<slot name="tip"/>
			<a-form-item ref="formItemRef" v-if="curInputType === 'textArea'" style="width: 90%;margin-left: 5%;">
				<a-textarea v-model:value="value" :placeholder="curPlaceHolder"
				    @change="onInputChange" @focus="onInputFocus" @blur="onInputBlur" auto-size id="text-area"/>
			</a-form-item>
			<a-form-item ref="formItemRef" v-if="curInputType === 'password'" style="width: 90%;margin-left: 5%;margin-top: 1rem;">
				<a-input-password v-model:value="value" :placeholder="curPlaceHolder"
				    @change="onInputChange" @focus="onInputFocus" @blur="onInputBlur" auto-size id="password"/>
			</a-form-item>
			<a-form-item ref="formItemRef" v-if="curInputType === 'datePicker'" style="width: 90%;margin-left: 5%;">
				<van-datetime-picker v-model="value" :min-date="minDate" :show-toolbar="false"
				    @change="onInputChange" @focus="onInputFocus" @blur="onInputBlur" id="date-picker"/>
			</a-form-item>
		</slot>
	</van-dialog>
</template>
<script>
import global from '@/lib/js/global';
import {clearError, makeError} from '@/lib/js/uitls/form-util';
import {toRef} from 'vue';

export default {
	name: 'PromptDialog',
	props: {
		title: {
			type: String,
			default: '输入操作原因'
		},
		focusTitle: {
			type: String,
			default: ''
		},
		tipMessage: {
			type: String,
			default: ''
		},
		inputType: {
			type: String,
			default: 'textArea'
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
		},
		teleport: {
			type: String,
			default: 'body'
		}
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	emits: ['confirm'],
	data() {
		return {
			showDrawer: toRef(global.states.blog, 'showDrawer'),
			hasTemporaryClosed: false,
			curTitle: this.title,
			curFocusTitle: this.focusTitle,
			curTipMessage: this.tipMessage,
			curInputType: this.inputType,
			curPlaceHolder: this.placeholder,
			curOnValidate: this.onValidate,
			curErrorMessage: this.errorMessage,
			showDialog: false,
			oriValue: '',
			value: '',
			minDate: new Date()
		}
	},
	watch: {
		showDrawer(isShow) {
			if (this.lptContainer === 'blogDrawer') {
				if (isShow && this.hasTemporaryClosed) {
					this.showDialog = true;
					this.hasTemporaryClosed = false;
				}
				if (!isShow && this.showDialog) {
					this.showDialog = false;
					this.hasTemporaryClosed = true;
				}
			}
		}
	},
	created() {
		this.promptSymbol = Symbol();
	},
	methods: {
		getParam(param, name) {
			if (typeof param[name] !== 'undefined') {
				return param[name];
			} else {
				return this[name];
			}
		},
		prompt(param) {
			this.curTitle = this.getParam(param, 'title');
			this.curFocusTitle = this.getParam(param, 'focusTitle');
			this.curTipMessage = this.getParam(param, 'tipMessage');
			this.curInputType = this.getParam(param, 'inputType');
			this.curPlaceHolder = this.getParam(param, 'placeholder');
			this.curOnValidate = this.getParam(param, 'onValidate');
			this.curErrorMessage = this.getParam(param, 'errorMessage');
			this.oriValue = param.value;
			this.value = this.getParam(param, 'value');
			this.onConfirm = param.onConfirm;
			this.onCancel = param.onCancel;
			this.onFinish = param.onFinish;
			this.showDialog = true;
			if (this.curInputType === 'datePicker') {
				this.minDate = new Date();
			}
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

:global(.prompt-dialog .ant-col) {
	/* 用于解决ant-row下宽度只有一半的BUG */
	flex: 1;
}
</style>