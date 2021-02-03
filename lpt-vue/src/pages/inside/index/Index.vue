<template>
	<div style="height: 100%">
		<a-popover trigger="click" placement="bottomLeft" v-model:visible="showPopover">
			<template v-slot:content>
				<div style="width: 4rem">
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
			<transition name="fade" v-if="postListLoaded">
				<a-button id="sortTypeBtn" v-show="showSortTypeSelector" >
					<OrderedListOutlined/>
					<span style="margin-left: 0.5rem">
						<span v-if="sortType === 'popular'">热门</span>
						<span v-if="sortType === 'latest'">最新</span>
					</span>
				</a-button>
			</transition>
		</a-popover>
		<post-list ref="postList" keep-scroll-top class='post-list' :sort-type="sortType" :onLoaded="onPostListLoaded"/>
	</div>
</template>

<script>
import PostList from '@/components/post/PostList'
import {OrderedListOutlined} from '@ant-design/icons-vue';

export default {
	name: 'Index',
	components: {
		PostList,
		OrderedListOutlined
	},
	data() {
		return {
			sortType: 'popular',
			postListLoaded: false,
			showSortTypeSelector: false,
			showPopover: false
		}
	},
	mounted() {
		const listElem = this.$refs.postList.$el;
		let preScrollTop = 0;
		listElem.addEventListener('scroll', () => {
			const curScrollTop = listElem.scrollTop;
			const diff = curScrollTop - preScrollTop;
			preScrollTop = curScrollTop;
			if (diff > 0 && curScrollTop > 300) {
				this.showSortTypeSelector = false;
				this.showPopover = false;
			} else if (diff < 0) {
				this.showSortTypeSelector = true;
			}
		});
	},
	methods: {
		changeSortType(type) {
			this.sortType = type;
		},
		getPopBtnType(sortType) {
			return this.sortType === sortType ? 'primary' : 'default';
		},
		onPostListLoaded() {
			this.showSortTypeSelector = true;
			this.postListLoaded = true;
		}
	}
}
</script>

<style scoped>
.post-list {
	padding: 8px 0;
}

.pop-btn {
	border-top: none;
	border-left: none;
	border-right: none;
	width: 100%;
}

.pop-btn:last-of-type {
	border-bottom: none;
}

#sortTypeBtn {
	position: fixed;
	right: 1rem;
	top: 1.5rem;
	background-color: white;
	z-index: 2;
}

.fade-enter-active, .fade-leave-active {
	transition: opacity 0.7s ease;
}

.fade-enter-from, .fade-leave-to {
	opacity: 0;
}
</style>