<template>
	<div id="main-area" :style="{height: getScrollHeight(), position: 'relative'}">
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
			<transition name="van-fade" v-if="postListLoaded">
				<a-button id="sortTypeBtn" v-show="showSortTypeSelector" >
					<OrderedListOutlined/>
					<span style="margin-left: 0.5rem">
						<span v-if="sortType === 'popular'">热门</span>
						<span v-if="sortType === 'latest'">最新</span>
					</span>
				</a-button>
			</transition>
		</a-popover>
		<a-back-top :style="{bottom: (mainBarHeight + 10) + 'px'}" :target="getElement"/>
		<post-list ref="postList" keep-scroll-top :sort-type="sortType" @loaded="onPostListLoaded"/>
	</div>
</template>

<script>
import PostList from '@/components/post/PostList';
import {OrderedListOutlined} from '@ant-design/icons-vue';
import global from '@/lib/js/global';

export default {
	name: 'Index',
	components: {
		PostList,
		OrderedListOutlined
	},
	data() {
		return {
			mainBarHeight: global.vars.style.tabBarHeight,
			sortType: global.states.pages.index.sortType,
			postListLoaded: false,
			showSortTypeSelector: false,
			showPopover: false
		}
	},
	mounted() {
		const listElem = this.$el;
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
			this.showPopover = false;
		},
		getPopBtnType(sortType) {
			return this.sortType === sortType ? 'primary' : 'default';
		},
		onPostListLoaded() {
			this.showSortTypeSelector = true;
			this.postListLoaded = true;
		},
		getScrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			// 底部高度加0.5的padding
			const barHeight = this.mainBarHeight;
			return mainViewHeight - barHeight + 'px';
		},
		getElement() {
			return this.$el;
		}
	}
}
</script>

<style scoped>

#main-area {
	overflow-y: scroll;
}

#main-area::-webkit-scrollbar {
	display: none;
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
</style>