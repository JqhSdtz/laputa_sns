<template>
	<van-action-sheet v-model:show="showCapturePanel" :actions="actions" @select="onSelect"
	                  close-on-click-action cancel-text="取消" description="请选择上传方式"/>
	<van-form ref="form" style="margin-top: 1.5rem">
		<van-field :rules="rules.title" v-model="form.title" placeholder="输入标题（可选，最多20字符）"/>
		<van-field :rules="rules.content" v-model="form.content" type="textarea" placeholder="输入内容（必填，最多100000字符）"
		           :autosize="{minHeight: 100}"/>
		<van-uploader ref="uploader" class="uploader" v-model="fileList" @click="beforeChoose"
		              :before-read="parseUpload" multiple :max-count="maxImgCount"/>
		<van-cell center title="是否公开">
			<template #right-icon>
				<van-switch v-model="form.isPublic" size="24"/>
			</template>
		</van-cell>
		<van-field v-if="form.isPublic" :rules="rules.category" v-model="selectedCategory" is-link readonly label="目录"
		           placeholder="选择发布目录（必填）" @click="showPopover = true" style="margin-top: 1rem"/>
		<van-popup v-model:show="showPopover" round position="bottom">
			<van-cascader v-model="form.categoryId" title="选择发布目录" :options="categoryOptions"
			              @change="onCategorySelect" @finish="onCategorySelectFinish"
			              @close="showPopover = false"/>
		</van-popup>
	</van-form>
	<van-button @click="onSubmit" type="primary" round block style="width: 80%; margin-left: 10%; margin-top: 1rem">
		发布
	</van-button>
</template>

<script>
import {Toast} from "vant";
import lpt from '@/lib/js/laputa/laputa';
import uploadRequest from 'ant-design-vue/es/vc-upload/src/request'
import global from '@/lib/js/global';

export default {
	name: 'Publish',
	data() {
		return {
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
			selectedCategory: '',
			form: {
				title: '',
				content: '',
				categoryId: '',
				isPublic: false
			},
			rules: {
				title: [
					{
						trigger: 'onBlur',
						validator(value) {
							if (!value) {
								return true;
							} else if (value.length > 20) {
								return '标题不能超过20个字符';
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
						trigger: 'onBlur',
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
			}
		}
	},
	watch: {
		$route() {
			this.categoryOptions = [];
			this.parseQueryParam();
		},
		showPopover(isShow) {
			if (!isShow && this.curOption && !this.curOption.isLeaf) {
				this.form.categoryId = '';
			}
		}
	},
	created() {
		this.parseQueryParam();
		this.lptConsumer = lpt.createConsumer();
		this.imgUrlMap = new Map();
	},
	methods: {
		parseQueryParam() {
			const query = this.$route.query;
			this.form.isPublic = query.type === 'public';
			if (query.isCategoryLeaf && query.categoryId) {
				const categoryId = query.categoryId;
				global.states.categoryManager.get(categoryId, category => {
					const pathList = category.path_list;
					if (pathList.length == 0)
						return;
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
				});
			} else {
				this.form.categoryId = query.categoryId || '';
				this.selectedCategory = query.categoryPath || '';
				lpt.categoryServ.getRoots({
					consumer: this.lptConsumer,
					success: (result) => {
						result.object.forEach(category => {
							if (category.allow_user_post) {
								this.categoryOptions.push({
									text: category.name,
									value: category.id,
									isLeaf: category.is_leaf
								});
							}
						});
					}
				});
			}
		},
		getFullRawUrl() {
			const ref = this;
			let fullRawUrl = '';
			this.fileList.forEach((file, idx) => {
				const url = ref.imgUrlMap.get(file.file);
				fullRawUrl += idx === 0 ? url : ('#' + url);
			});
			return fullRawUrl
		},
		onSubmit() {
			const ref = this;
			this.$refs.form.validate().then(() => {
				lpt.postServ.create({
					consumer: ref.lptConsumer,
					param: {
						type: ref.form.isPublic ? 'public' : 'private'
					},
					data: {
						category_id: ref.form.categoryId,
						title: ref.form.title,
						content: ref.form.content,
						raw_img: ref.getFullRawUrl()
					},
					success: function () {
						ref.form.title = '';
						ref.form.content = '';
						ref.fileList = [];
						Toast.success('发布成功');
					},
					fail: function (result) {
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
			lpt.categoryServ.getDirectSub({
				consumer: this.lptConsumer,
				data: {
					categoryId: option.value
				},
				success(result) {
					option.children = result.object.map(category => {
						return {
							text: category.name,
							value: category.id,
							isLeaf: category.is_leaf
						}
					});
				}
			});
		},
		onCategorySelectFinish({selectedOptions}) {
			this.curOption = selectedOptions[selectedOptions.length - 1];
			if (this.curOption.isLeaf) {
				this.showPopover = false;
				this.selectedCategory = selectedOptions.map((option) => option.text).join('/');
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
					action: lpt.baseUrl + '/oss/pst',
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
		}
	}
}
</script>

<style scoped>
.uploader {
	margin: 1rem
}
</style>