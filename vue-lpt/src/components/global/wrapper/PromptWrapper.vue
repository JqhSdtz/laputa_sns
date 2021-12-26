<template>
    <slot></slot>
    <template v-if="hasMounted">
        <prompt-dialog ref="categoryPrompt" :teleport="teleport">
            <template v-if="showSelectedCategory" v-slot:tip>
                <category-grid-item :category-id="curSelectedCategory.id"/>
            </template>
        </prompt-dialog>
        <prompt-dialog ref="userPrompt" :teleport="teleport">
            <template v-if="showSelectedUser" v-slot:tip>
                <user-item :show-id="true" :user="curSelectedUser"/>
            </template>
        </prompt-dialog>
        <prompt-dialog ref="plainPrompt" :teleport="teleport"/>
        <prompt-dialog ref="confirmPrompt" :teleport="teleport"/>
        <prompt-dialog ref="alertPrompt" :teleport="teleport"
            :tip-message-style="{margin: '1rem'}" 
            :show-cancel-button="false"/>
    </template>
</template>

<script>
import PromptDialog from '@/components/global/PromptDialog';
import CategoryGridItem from "@/components/category/item/CategoryGridItem";
import UserItem from '@/components/user/item/UserItem';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';

export default {
    name: 'PromptWrapper',
    components: {
        PromptDialog,
        CategoryGridItem,
        UserItem
    },
    inject: {
		lptContainer: {
			type: String
		}
	},
    provide() {
        return {
            prompts: this.prompts
        }
    },
    data() {
        return {
            prompts: {},
            curSelectedCategory: lpt.categoryServ.getDefaultCategory(-1),
			curSelectedUser: lpt.userServ.getDefaultUser(-1),
            showSelectedUser: false,
			showSelectedCategory: false,
            hasMounted: false,
            teleport: this.lptContainer === 'blogDrawer' ? '#blog-drawer .ant-drawer-content-wrapper' : undefined
        }
    },
    mounted() {
        this.hasMounted = true;
        this.$nextTick(() => this.bindPrompt());
    },
    methods: {
        bindPrompt() {
            const that = this;
            this.prompts.plainPrompt = this.$refs.plainPrompt.prompt;
            function paramsProcess(params) {
                params.title = params.title || '';
                params.inputType = 'none';
                params.tipMessage = params.message;
                params.onValidate = params.onValidate || (() => true);
            }
            this.prompts.alert = function(params) {
                paramsProcess(params);
                that.$refs.alertPrompt.prompt.apply(this, arguments);
            };
            this.prompts.confirm = function(params) {
                paramsProcess(params);
                that.$refs.confirmPrompt.prompt.apply(this, arguments);
            };
            this.prompts.userPrompt = (paramsFun) => {
                const prompt = this.$refs.userPrompt.prompt;
                this.showSelectedUser = false;
                prompt({
                    title: '输入目标用户的用户名',
                    placeholder: ' ',
                    onValidate: (value) => {
                        return lpt.userServ.getByName({
                            consumer: this.lptConsumer,
                            throwError: true,
                            param: {
                                userName: value
                            },
                            success: (result) => {
                                this.curSelectedUser = result.object;
                            }
                        }).catch(() => {
                            return Promise.reject(`用户"${value}"不存在`);
                        });
                    },
                    onConfirm: () => {
                        const params = paramsFun(this.curSelectedUser, function() {
                            that.showSelectedUser = false;
                            return prompt.apply(this, arguments);
                        });
                        const oriOnFinish = params.onFinish;
                        params.onFinish = function() {
                            that.showSelectedUser = false;
                            oriOnFinish && oriOnFinish.apply(this, arguments);
                        }
                        this.showSelectedUser = true;
                        return prompt(params);
                    }
                });
            };
            this.prompts.categoryPrompt = (paramsFun) => {
                const prompt = this.$refs.categoryPrompt.prompt;
                this.showSelectedCategory = false;
                prompt({
                    title: '输入目标目录的ID',
                    placeholder: ' ',
                    onValidate: (value) => {
                        return global.states.categoryManager.get({
                            itemId: value,
                            getPromise: true
                        }).then((category) => {
                            this.curSelectedCategory = category;
                        }).catch(() => {
                            return Promise.reject(`ID为"${value}"的目录不存在`);
                        });
                    },
                    onConfirm: () => {
                        const params = paramsFun(this.curSelectedCategory, function() {
                            that.showSelectedCategory = false;
                            return prompt.apply(this, arguments);
                        });
                        const oriOnFinish = params.onFinish;
                        params.onFinish = function() {
                            that.showSelectedCategory = false;
                            oriOnFinish && oriOnFinish.apply(this, arguments);
                        }
                        this.showSelectedCategory = true;
                        return prompt(params);
                    }
                });
            };
        }
    }
}
</script>