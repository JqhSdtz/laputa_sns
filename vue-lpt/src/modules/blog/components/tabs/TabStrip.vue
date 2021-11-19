<template>
	<div id="tab-container" :style="{height: outerHeight}">
		<div id="tabs-separator"></div>
		<div id="tab-container-inner" :style="{height: scrollHeight}">
			<div class="tab-item" v-for="(tabItem, index) in tabItems" 
				:key="tabItem.path"
				:class="{active: activePath === tabItem.path}"
				@click="onTabItemClick(tabItem)">
				<span class="tab-title" v-text="tabItem.title"></span>
				<van-icon name="cross" class="close-icon" @click.capture.stop="onCloseIconClick(index)"/>
				<!-- 清除浮动 -->
				<div style="clear: both;"></div>
			</div>
		</div>
		<div style="position: absolute; text-align: center;width: 100%;bottom: 0">
			<van-icon name="cross" id="close-all-icon" class="close-icon" @click="onCloseAllIconClick()"/>
		</div>
	</div>
</template>

<script>
import global from '@/lib/js/global';
import remHelper from '@/lib/js/uitls/rem-helper';

export default {
	name: 'TabStrip',
	components: {},
	data() {
		const tabItems = [];
		this.pathTitleMap = new Map();
		return {
			tabItems,
			activePath: ''
		};
	},
	computed: {
		outerHeight() {
			return global.states.style.bodyHeight - remHelper.remToPx(5) + 'px';
		},
		scrollHeight() {
			// 距顶部高度是5rem，这里留7是为了给底部留空间
			return global.states.style.bodyHeight - remHelper.remToPx(7.5) + 'px';
		}
	},
	created() {
		global.events.on('blogMainRoute', (path) => {
			if (!this.tabItems.some(item => item.path === path)) {
				// 不存在该路径的tab页再添加，防止死循环
				const title = this.pathTitleMap.get(path) || path;
				this.tabItems.push({
					path: path,
					title: title,
				});
			}
			this.activePath = path;
		});
		global.events.on('onSetBlogTitle', (param) => {
			const target = this.tabItems.find((item) => item.path === param.path);
			if (target) {
				target.title = param.title;
				// 将该次的path和title关系保存下来，便于下次设置title
				// 也可以防止回退时不出发setTitle而导致标签名称为path的BUG
				this.pathTitleMap.set(param.path, param.title);
			}
		});
	},
	methods: {
        onTabItemClick(tabItem) {
			this.activePath = tabItem.path;
			this.$router.push({
				path: tabItem.path
			});
        },
		onCloseIconClick(index) {
			this.tabItems.splice(index, 1);
		},
		onCloseAllIconClick() {
			this.tabItems.splice(0, this.tabItems.length);
		}
	},
};
</script>

<style scoped>
#tab-container {
	position: absolute;
	left: 0;
	top: 5rem;
	width: 10%;
}

#tab-container-inner {
	margin-top: 0.75rem;
	width: 80%;
	margin-left: 10%;
	overflow-y: scroll;
}

#tabs-separator {
	height: 0.1rem;
	width: 100%;
	background-color: rgba(255, 255, 255, 0.1);
}

.tab-item {
	margin: 0.5rem 0;
	padding: 0.25rem 0.5rem;
	word-break: break-all;
	background-color: rgba(255, 255, 255, 0.1);
	box-shadow: 0 0 5px 3px rgba(0, 0, 0, 0.1);
}

.tab-item:hover,.tab-item.active {
	background-color: rgba(255, 255, 255, 0.5);
	box-shadow: 0 0 5px 3px rgba(0, 0, 0, 0.25);
}

.close-icon {
	float: right;
	line-height: 1.6rem;
	cursor: pointer;
	transition: font-size 0.2s cubic-bezier(0.7, 0.3, 0.1, 1);
}

.close-icon:hover {
	font-size: 1.25rem;
}

#close-all-icon {
	font-size: 1.25rem;
	color: rgba(0, 0, 0, 0.5);
	float: none;
}

#close-all-icon:hover {
	font-size: 2rem;
	color: rgba(0, 0, 0, 0.85);
}
</style>