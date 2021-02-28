<template>
	<div id="publish-btn" :style="{top: topOffset}">
		<transition name="van-fade">
			<edit-filled v-show="show" id="edit-icon" @click="goPublish"/>
		</transition>
	</div>
</template>

<script>
import {EditFilled} from '@ant-design/icons-vue';
import remHelper from '@/lib/js/uitls/rem-helper';
import lpt from '@/lib/js/laputa/laputa';

export default {
	name: 'FloatPublishButton',
	props: {
		hideOffsetBase: {
			type: Number,
			default: 0
		},
		autoHide: {
			type: Boolean,
			default: true
		},
		offset: {
			type: String,
			default: '4rem'
		},
		category: Object
	},
	components: {
		EditFilled
	},
	data() {
		return {
			show: true
		}
	},
	computed: {
		topOffset() {
			const mainViewHeight = document.body.clientHeight;
			let offset;
			if (this.offset.indexOf('rem') > 0) {
				offset = remHelper.remToPx(parseInt(this.offset));
			} else {
				offset = parseInt(this.offset);
			}
			return mainViewHeight - offset + 'px';
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
					this.show = false;
				}
			} else if (diff < 0) {
				this.show = true;
			}
		});
	},
	methods: {
		goPublish() {
			this.$router.push({
				name: 'publish',
				query: {
					type: 'public',
					categoryId: this.category.id,
					isCategoryLeaf: this.category.is_leaf,
					categoryName: this.category.name,
					categoryPath: lpt.categoryServ.getPathStr(this.category.path_list)
				}
			});
		}
	}
}
</script>

<style scoped>
#publish-btn {
	position: fixed;
	z-index: 1
}

#edit-icon {
	font-size: 2rem;
}
</style>