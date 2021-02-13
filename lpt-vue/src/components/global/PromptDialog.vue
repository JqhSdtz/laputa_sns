<template>
	<van-dialog v-model:show="showDialog" :title="title" :before-close="beforeClose" show-cancel-button>
		<a-form-item ref="formItemRef">
			<a-textarea v-model:value="value" :placeholder="placeholder"
			            @change="onInputChange" auto-size/>
		</a-form-item>
	</van-dialog>
</template>
<script>
import {clearError, makeError} from "@/lib/js/uitls/form-util";

export default {
	name: 'PromptDialog',
	props: {
		show: Boolean,
		title: {
			type: String,
			default: '输入操作原因'
		},
		placeholder: {
			type: String,
			default: '请输入操作原因(5-256个字)'
		}
	},
	watch: {
		show(newValue) {
			this.showDialog = newValue;
		}
	},
	emits: ['update:show', 'confirm'],
	data() {
		const ref = this;
		return {
			showDialog: this.show,
			value: '',
			beforeClose(action) {
				if (action === 'cancel') {
					ref.$emit('update:show', false);
					return;
				}
				const value = ref.value;
				if (value.length < 5 || value.length > 256) {
					const inputRef = ref.$refs.formItemRef;
					makeError(inputRef, '输入内容长度应在5-256个字之间');
				} else {
					ref.$emit('update:show', false);
					ref.$emit('confirm', value);
					ref.value = '';
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
	}
}
</script>

<style scoped>

</style>