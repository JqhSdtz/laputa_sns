<template>
	<div>
		<a-popover trigger="click" placement="bottomLeft" v-model:visible="showPopover">
			<template v-slot:content>
				<div style="font-size: 0.9rem">
					<a-button class="pop-btn" :type="getPopBtnType('popular')"
					          @click="changeSortType('popular')">
						热门
					</a-button>
					<a-button class="pop-btn" :type="getPopBtnType('latest')"
					          @click="changeSortType('latest')">
						最新
					</a-button>
				</div>
			</template>
			<transition name="van-fade">
				<a-button class="sort-type-btn" ref="sortTypeButton" 
						  v-show="showSortTypeSelector"
				          :style="defButtonStyle"
				          style="font-size: 0.9rem"
						  @mouseenter="onSortTypeButtonMouseEnter"
						  @mouseleave="onSortTypeButtonMouseLeave">
					<ordered-list-outlined/>
					<span style="margin-left: 0.5rem">
						<span v-if="sortType === 'popular'">热门</span>
						<span v-if="sortType === 'latest'">最新</span>
					</span>
				</a-button>
			</transition>
		</a-popover>
	</div>
</template>

<script>
import {OrderedListOutlined} from '@ant-design/icons-vue';
import global from '@/lib/js/global';

export default {
	name: 'SortTypeSelector',
	components: {
		OrderedListOutlined
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	props: {
		hideOffsetBase: {
			type: Number,
			default: 0
		},
		autoHide: {
			type: Boolean,
			default: true
		},
		buttonStyle: Object,
		offset: {
			type: String,
			default: '4.5rem'
		},
		position: {
			type: String,
			default: 'inside'
		},
		sortType: String
	},
	emits: ['update:sortType'],
	data() {
		const tmpButtonStyle = {};
		if (this.position === 'left') {
			tmpButtonStyle.left = '2%';
			tmpButtonStyle.backgroundColor = 'rgba(255, 255, 255, 0.5)';
			tmpButtonStyle.border = 'none';
		} else {
			tmpButtonStyle.right = '1rem';
		}
		return {
			defButtonStyle: {
				top: this.offset,
				...tmpButtonStyle,
				...this.buttonStyle
			},
			showSortTypeSelector: true,
			showPopover: false
		}
	},
	mounted() {
		const listElem = this.$parent.$el;
		let preScrollTop = 0;
		listElem.addEventListener('scroll', () => {
			const curScrollTop = listElem.scrollTop;
			const diff = curScrollTop - preScrollTop;
			preScrollTop = curScrollTop;
			if (diff > 0 && curScrollTop - this.hideOffsetBase > 300) {
				if (this.autoHide) {
					this.showSortTypeSelector = false;
					this.showPopover = false;
				}
			} else if (diff < 0) {
				this.showSortTypeSelector = true;
			}
		});
		if (this.lptContainer === 'blogMain' && this.position !== 'inside') {
			// 在blogMain环境下，按钮不在页面中，但position还是fixed，无法随页面整体移动
			// 所以用监听页面scroll事件的方式保持按钮和页面一起移动
			const elem = this.$refs.sortTypeButton.$el;
			window.addEventListener('scroll', () => {
				elem.style.top = global.methods.parsePxSize(this.offset) - window.scrollY + 'px';
			});
		}
	},
	methods: {
		changeSortType(type) {
			this.$emit('update:sortType', type);
			this.showPopover = false;
		},
		getPopBtnType(sortType) {
			return this.sortType === sortType ? 'primary' : 'default';
		},
		onSortTypeButtonMouseEnter(event) {
			const elem = event.target;
			if (this.lptContainer === 'blogMain') {
				elem.style.backgroundColor = 'rgba(255, 255, 255, 1)';
			}
		},
		onSortTypeButtonMouseLeave(event) {
			const elem = event.target;
			if (this.lptContainer === 'blogMain') {
				elem.style.backgroundColor = 'rgba(255, 255, 255, 0.5)';
			}
		}
	}
}
</script>

<style scoped>
.pop-btn {
	border-top: none;
	border-left: none;
	border-right: none;
	width: 100%;
}

.pop-btn:last-of-type {
	border-bottom: none;
}

.sort-type-btn {
	font-size: 0.9rem;
	position: fixed;
	z-index: 2;
	transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1), top 0s;
}
</style>