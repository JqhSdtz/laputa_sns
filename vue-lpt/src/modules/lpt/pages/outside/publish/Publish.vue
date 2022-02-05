<template>
	<text-editor v-show="textEditor.show && textEditor.type === 'abstract'" 
			v-model:text="form.abstract"
			title="编辑摘要" 
			maxlength="256"
			@close="textEditor.show = false"/>
	<text-editor v-show="textEditor.show && textEditor.type === 'content'" 
			v-model:text="form.content"
			title="编辑内容" 
			maxlength="100000"
			@close="textEditor.show = false"/>
	<div v-show="!textEditor.show">
		<van-action-sheet v-model:show="showCapturePanel" :actions="actions" @select="onSelect"
		        cancel-text="取消" description="请选择上传方式"/>
		<van-form ref="form" style="margin-top: 1.5rem">
			<van-field :rules="rules.title" v-model="form.title" 
					placeholder="输入标题（可选，最多40字符）"/>
			<van-cell title="编辑摘要" is-link @click="editAbstract"/>
			<van-field :rules="rules.abstract" v-model="form.abstract" type="textarea" 
					maxlength="256" 
					:show-word-limit="true" 
					placeholder="输入摘要（可选，最多256字符）"/>
			<van-cell title="编辑内容" is-link @click="editContent"/>
			<van-field :rules="rules.content" v-model="form.content" type="textarea" 
					maxlength="100000" 
					:show-word-limit="true" 
					placeholder="输入内容（必填，最多100000字符）"/>
			<van-uploader ref="uploader" class="uploader" v-model="fileList" @click="beforeChoose"
			        :before-read="parseUpload" multiple :max-count="maxImgCount"
					:preview-options="{closeable: true}"/>
			<van-cell center title="是否公开">
				<template #right-icon>
					<van-switch v-model="form.isPublic" :disabled="opType !== 'create'" size="24"/>
				</template>
			</van-cell>
			<van-cell v-if="selectedCategory.rights.create_editable_post" center title="是否可编辑">
				<template #right-icon>
					<van-switch v-model="form.editable" :disabled="opType !== 'create'" size="24"/>
				</template>
			</van-cell>
			<van-cell center title="是否使用MarkDown">
				<template #right-icon>
					<van-switch v-model="form.useMarkDown" size="24" @change="onUseMarkDownChange"/>
				</template>
			</van-cell>
			<van-field v-if="form.isPublic" :disabled="opType !== 'create'" :rules="rules.category"
			        v-model="selectedCategoryPathText" is-link readonly label="目录"
			        placeholder="选择发布目录（必填）" @click="showPopover = true" style="margin-top: 1rem"/>
			<van-popup v-if="opType === 'create'" v-model:show="showPopover" round
			        position="bottom" :style="{width: clientWidth + 'px'}">
				<van-cascader v-model="form.categoryId" title="选择发布目录" :options="categoryOptions"
				        @change="onCategorySelect" @finish="onCategorySelectFinish"
				        @close="showPopover = false"/>
			</van-popup>
		</van-form>
		<van-button @click="onSubmit" type="primary" round block style="width: 80%; margin: 1rem 10%">
			发布
		</van-button>
	</div>
</template>

<script>
import {Dialog, Toast} from "vant";
import TextEditor from './TextEditor';
import lpt from '@/lib/js/laputa/laputa';
import uploadRequest from 'ant-design-vue/es/vc-upload/src/request';
import global from '@/lib/js/global';

export default {
	name: 'Publish',
	components: {
		TextEditor
	},
	inject: {
		lptContainer: {
			type: String
		},
		prompts: {
			type: Object
		}
	},
	data() {
		return {
			postId: '',
			opType: '',
			showCapturePanel: false,
			maxImgCount: 9,
			actions: [
				{
					id: 'camera',
					name: '拍照'
				}, {
					id: 'gallery',
					name: '相册'
				}
			],
			fileList: [],
			showPopover: false,
			categoryOptions: [],
			selectedCategory: lpt.categoryServ.getDefaultCategory(-1),
			selectedCategoryPath: [],
			selectedCategoryPathText: '',
			form: {
				title: '',
				abstract: '',
				content: '',
				categoryId: '',
				useMarkDown: false,
				editable: false,
				isPublic: false
			},
			rules: {
				title: [
					{
						trigger: 'onChange',
						validator(value) {
							if (!value) {
								return true;
							} else if (value.length > 40) {
								return '标题不能超过40个字符';
							} else {
								return true;
							}
						}
					}
				],
				abstract: [
					{
						trigger: 'onChange',
						validator(value) {
							if (!value) {
								return true;
							} else if (value.length < 10) {
								return '摘要不能少于10个字符';
							} else if (value.length > 256) {
								return '摘要不能超过256个字符';
							} else {
								return true;
							}
						}
					}
				],
				content: [
					{
						required: true,
						message: '请输入内容'
					},
					{
						trigger: 'onChange',
						validator(value) {
							if (!value) {
								return false;
							} else if (value.length < 10) {
								return '内容不能少于10个字符';
							} else if (value.length > 100000) {
								return '内容不能多于100000个字符'
							} else {
								return true;
							}
						}
					}
				],
				category: [
					{
						required: true,
						message: '请选择发布目录'
					}
				]
			},
			textEditor: {
				show: false,
				maxlength: 0,
				type: '',
				title: ''
			}
		}
	},
	watch: {
		showPopover(isShow) {
			if (!isShow && this.curOption && !this.curOption.isLeaf) {
				this.form.categoryId = '';
			}
		},
		$route(route) {
			if (route.name === 'publish') this.onActivated();
		},
		'form.abstract'(newValue, oldValue) {
			if (!oldValue && !this.checkIsTpMd(newValue) && this.form.useMarkDown) {
				this.form.abstract = 'tp:md#' + this.form.abstract;
			}
			this.checkInputContent();
		},
		'form.content'(newValue, oldValue) {
			if (!oldValue && !this.checkIsTpMd(newValue) && this.form.useMarkDown) {
				this.form.content = 'tp:md#' + this.form.content;
			}
			this.checkInputContent();
		}
	},
	computed: {
		clientWidth() {
			if (this.lptContainer === 'blogDrawer') {
				return global.states.style.drawerWidth;
			} else if (this.lptContainer === 'blogMain') {
				return global.states.style.blogMainWidth;
			} else {
				return global.states.style.bodyWidth;
			}
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.imgUrlMap = new Map();
		if (this.lptContainer !== 'blogDrawer') {
			this.onActivated();
		}
		global.events.on('showDrawer', () => {
			// blog环境下，从外部打开drawer中的publish页面，不会触发$route的监听
			// 所以采用全局事件系统解决该问题
			const routeName = this.$route.name;
			if (routeName === 'publish' || routeName.indexOf('_publish') > 0) {
				this.onActivated();
			}
		});
	},
	methods: {
		onActivated() {
			const f = this.form;
			if (!f.title && !f.content && !f.abstract && this.fileList.length === 0) {
				// 当前无内容，则直接解析参数
				this.$nextTick(() => this.parseQueryParam());
			} else if (this.$route.fullPath !== this.preHref) {
				// 当前有内容，并且参数有变化
				this.prompts.confirm({
					title: '是否清空',
					message: '当前有未提交内容，是否清空内容？',
					onConfirm: () => {
						this.preHref = this.$route.fullPath;
						this.form.title = '';
						this.form.content = '';
						this.form.abstract = '';
						this.fileList = [];
						this.form.categoryId = '';
						this.selectedCategoryPathText = '';
						this.selectedCategory = lpt.categoryServ.getDefaultCategory(-1);
						this.parseQueryParam();
					}
				});
			}
		},
		parseCreatePostParam() {
			const query = this.$route.query;
			this.form.isPublic = query.type === 'public';
			if (query.isCategoryLeaf && query.categoryId) {
				const categoryId = query.categoryId;
				global.states.categoryManager.get({
					itemId: categoryId,
					success: (category) => {
						this.selectedCategory = category;
						const pathList = category.path_list;
						if (pathList.length == 0)
							return;
						this.categoryOptions = [];
						let option = this.categoryOptions;
						let lastOption = {};
						pathList.forEach(category => {
							if (category.id === lpt.categoryServ.rootCategoryId)
								return;
							const children = [];
							option.push({
								text: category.name,
								value: category.id,
								isLeaf: false,
								children
							});
							lastOption = option;
							option = children;
						});
						if (!category.is_leaf) {
							category.sub_list.forEach(category => {
								option.push({
									text: category.name,
									value: category.id,
									isLeaf: category.is_leaf
								});
							});
						} else {
							lastOption[0].isLeaf = true;
							delete lastOption[0].children;
						}
						this.form.categoryId = parseInt(categoryId);
						this.showPopover = true;
					}
				});
			} else {
				this.form.categoryId = query.categoryId || '';
				this.selectedCategoryPathText = query.categoryPath || '';
				this.categoryOptions = [];
				global.states.categoryManager.get({
					itemId: lpt.categoryServ.rootCategoryId,
					success: (item) => {
						item.sub_list.forEach(category => {
							if (this.hasPublishRight(category)) {
								this.categoryOptions.push({
									text: category.name,
									value: category.id,
									isLeaf: category.is_leaf
								});
							}
						});
					},
					fail(result) {
						Toast.fail(result.message);
					}
				});
			}
		},
		hasPublishRight(category) {
			if (!category.allow_user_post) {
				return false;
			} else if (typeof category.allow_post_level !== 'undefined') {
				const this_level = category.rights && category.rights.this_level;
				return this_level && this_level >= category.allow_post_level;
			} else {
				return true;
			}
		},
		parseEditPostParam() {
			const query = this.$route.query;
			const postId = query.postId;
			this.postId = postId;
			global.states.postManager.get({
				itemId: postId,
				success: (post) => {
					this.form.isPublic = post.type_str === 'public';
					this.selectedCategoryPathText = lpt.categoryServ.getPathStr(post.category_path);
					global.states.categoryManager.get({
						itemId: post.category_id,
						filter: res => res.rights && typeof res.rights.this_level !== 'undefined',
						success: (category) => {
							this.selectedCategory = category;
						}
					});
					this.form.editable = post.editable;
					this.form.title = post.title;
					if (post.full_text_id) {
						lpt.postServ.getFullText({
							param: {
								fullTextId: post.full_text_id
							},
							success: (result) => {
								this.form.abstract = post.content;
								this.form.content = result.object;
							}
						});
					} else {
						this.form.content = post.content;
					}
					const rawImg = post.raw_img;
					if (rawImg) {
						rawImg.split('#').forEach(str => {
							this.fileList.push({
								rawImg: str,
								url: lpt.getPostThumbUrl(str),
								isImage: true
							});
						});
					}
				}
			});
		},
		parseQueryParam() {
			const query = this.$route.query;
			const isQueryEmpty = Object.keys(query).length === 0;
			if (isQueryEmpty && this.hasInited) return;
			this.hasInited = true;
			this.preHref = this.$route.fullPath;
			query.opType = query.opType || 'create';
			this.opType = query.opType;
			if (query.opType === 'edit') {
				this.parseEditPostParam();
			} else {
				this.parseCreatePostParam();
			}
		},
		getFullRawUrl() {
			const ref = this;
			let fullRawUrl = '';
			this.fileList.forEach((file, idx) => {
				let url = file.rawImg;
				if (!url) {
					url = ref.imgUrlMap.get(file.file);
				}
				fullRawUrl += idx === 0 ? url : ('#' + url);
			});
			return fullRawUrl
		},
		checkIsTpMd(str) {
			return str && str.length >= 6 && str.substring(0, 6) === 'tp:md#';
		},
		checkInputContent() {
			this.form.useMarkDown = this.checkIsTpMd(this.form.abstract) || this.checkIsTpMd(this.form.content);
		},
		onUseMarkDownChange(value) {
			if (value) {
				if (this.form.abstract && !this.checkIsTpMd(this.form.abstract))
					this.form.abstract = 'tp:md#' + this.form.abstract;
				if (this.form.content && !this.checkIsTpMd(this.form.content))
					this.form.content = 'tp:md#' + this.form.content;
			} else {
				if (this.checkIsTpMd(this.form.abstract))
					this.form.abstract = this.form.abstract.substring(6);
				if (this.checkIsTpMd(this.form.content))
					this.form.content = this.form.content.substring(6);
			}
		},
		onSubmit() {
			this.$refs.form.validate().then(() => {
				let fun = () => {
				};
				if (this.opType === 'create') {
					fun = lpt.postServ.create;
				} else if (this.opType === 'edit') {
					fun = lpt.postServ.updateContent;
				}
				const data = {
					category_id: this.form.categoryId,
					title: this.form.title,
					content: this.form.content,
					editable: this.form.editable,
					raw_img: this.getFullRawUrl()
				};
				if (this.form.abstract) {
					// 有设置摘要
					data.content = this.form.abstract;
					data.full_text = this.form.content;
				}
				if (this.opType === 'edit') {
					data.id = this.postId;
				}
				data.type_str = this.form.isPublic ? 'public' : 'private';
				data.creator = global.states.curOperator.user;
				data.creator_id = global.states.curOperator.user.id;
				data.entity_type = 'POST';
				fun({
					consumer: this.lptConsumer,
					param: {
						type: data.type_str
					},
					data: data,
					success: (result) => {
						this.form.title = '';
						this.form.content = '';
						this.form.abstract = '';
						this.fileList = [];
						if (this.opType === 'create') {
							Toast.success('发布成功');
							data.category_path = this.selectedCategoryPath.reverse();
							data.rights = {
								delete: true,
								edit: this.form.editable
							};
							data.id = result.object;
							data.like_cnt = 0;
							data.comment_cnt = 0;
							data.forward_cnt = 0;
							data.create_time = new Date().getTime();
							const post = global.states.postManager.add(data);
							global.events.emit('createPost', post);
						} else if (this.opType === 'edit') {
							global.states.postManager.add(data);
							Toast.success('编辑成功');
						}
					},
					fail(result) {
						Toast.fail(result.message);
					}
				});
			}).catch(() => {
			});
		},
		onCategorySelect({selectedOptions}) {
			const option = selectedOptions[selectedOptions.length - 1];
			if (option.isLeaf)
				return;
			global.states.categoryManager.get({
				itemId: option.value,
				filter: (res) => res.sub_list,
				success: (item) => {
					option.children = [];
					item.sub_list.forEach(category => {
						if (this.hasPublishRight(category)) {
							option.children.push({
								text: category.name,
								value: category.id,
								isLeaf: category.is_leaf
							});
						}
					});
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		},
		onCategorySelectFinish({selectedOptions}) {
			this.curOption = selectedOptions[selectedOptions.length - 1];
			if (this.curOption.isLeaf) {
				this.showPopover = false;
				this.selectedCategoryPath = selectedOptions.map((option) => {
					return {
						id: option.value,
						name: option.text
					}
				});
				this.selectedCategoryPathText = selectedOptions.map((option) => option.text).join('/');
				global.states.categoryManager.get({
					itemId: this.curOption.value,
					success: category => {
						this.selectedCategory = category;
					},
					filter: (res) => res.rights
				});
			}
		},
		onSelect(item) {
			const inputElem = this.$refs.uploader.$el.getElementsByTagName('input')[0];
			if (item.id === 'camera') {
				inputElem.setAttribute('capture', 'camera');
			} else {
				inputElem.removeAttribute('capture');
			}
			inputElem.click();
			this.showCapturePanel = false;
		},
		beforeChoose(event) {
			if (this.fileList.length >= this.maxImgCount) {
				return;
			}
			if (!this.showCapturePanel) {
				this.showCapturePanel = true;
				event.preventDefault();
			}
		},
		doUpload(file) {
			const ref = this;
			if (this.fileList.length >= this.maxImgCount) {
				Toast.fail(`最多上传${this.maxImgCount}张图片`);
				return false;
			}
			if (file.type.indexOf('image') < 0) {
				Toast.fail('只能上传图片文件');
				return false;
			}
			if (file.size > 10485760) {
				Toast.fail('上传图片不能大于10M');
				return false;
			}
			global.events.emit('pushGlobalBusy', {
				isBusy: true
			});
			return new Promise((resolve, reject) => {
				const requestOption = {
					action: lpt.baseApiUrl + '/oss/pst',
					method: 'post',
					filename: 'file',
					data: {},
					file: file,
					headers: {
						'x-lpt-user-token': lpt.getCurUserToken()
					},
					withCredentials: true,
					onSuccess(result) {
						global.events.emit('pushGlobalBusy', {
							isBusy: false
						});
						if (result.state === 1) {
							ref.imgUrlMap.set(file, result.object);
							resolve(file);
						} else {
							reject(result);
						}
					},
					onError(err) {
						global.events.emit('pushGlobalBusy', {
							isBusy: false
						});
						reject(err);
					}
				};
				uploadRequest(requestOption);
			});
		},
		parseUpload(obj) {
			if (obj instanceof Array) {
				if (obj.length + this.fileList.length > this.maxImgCount) {
					const remain = this.maxImgCount - this.fileList.length;
					obj.splice(remain);
				}
				const promiseList = [];
				for (const file of obj) {
					const res = this.doUpload(file);
					if (!res) {
						return false;
					} else if (res instanceof Promise) {
						promiseList.push(res);
					}
				}
				if (promiseList.length > 0) {
					return Promise.all(promiseList);
				}
			} else {
				return this.doUpload(obj);
			}
		},
		editAbstract() {
			this.textEditor.show = true;
			this.textEditor.type = 'abstract';
		},
		editContent() {
			this.textEditor.show = true;
			this.textEditor.type = 'content';
		}
	}
}
</script>

<style scoped>
.uploader {
	margin: 1rem
}

:global(.van-uploader__input) {
	display: none;
}
</style>