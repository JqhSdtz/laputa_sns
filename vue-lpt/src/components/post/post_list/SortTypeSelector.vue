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
				<a-button class="sort-type-btn" v-show="showSortTypeSelector"
				          :style="{top: offset, right: offsetRight, ...buttonStyle}"
				          style="font-size: 0.9rem">
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
		sortType: String
	},
	emits: ['update:sortType'],
	data() {
		return {
			offsetRight: this.lptContainer === 'blogMain' ? '11%' : '1rem',
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
	},
	methods: {
		changeSortType(type) {
			this.$emit('update:sortType', type);
			this.showPopover = false;
		},
		getPopBtnType(sortType) {
			return this.sortType === sortType ? 'primary' : 'default';
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
	position: fixed;
	background-color: white;
	z-index: 2;
}
</style>