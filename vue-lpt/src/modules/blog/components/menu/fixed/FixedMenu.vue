<template>
	<div id="menu-container">
		<div id="menu-container-inner">
			<div class="menu-item" v-for="menuItem in menuItems" 
				:key="menuItem.type + (menuItem.id || '')"
				@mouseenter="onMenuItemMouseEnter($event, menuItem)"
				@mouseleave="onMenuItemMouseLeave($event, menuItem)"
				@click="onMenuItemClick(menuItem)">
				<p class="menu-title">
					<a-badge v-if="menuItem.showDot" :dot="true" :offset="[0, -2]"
						:numberStyle="{boxShadow: '0 0 0 1px #ff4d4f'}">
						<span class="menu-name" v-text="menuItem.name"></span>
					</a-badge>
					<template v-else>
						<span class="menu-name" v-text="menuItem.name"></span>
					</template>
				</p>
				<div class="menu-bar"></div>
				<div v-if="menuItem.separator" class="menu-separator"></div>
			</div>
		</div>
	</div>
</template>

<script>
import global from '@/lib/js/global';
import blogDescription from '@/modules/blog/description';
import remHelper from '@/lib/js/uitls/rem-helper';
import lpt from '@/lib/js/laputa/laputa';
import {toRef} from 'vue';

export default {
	name: 'FixedMenu',
	data() {
		const menuItems = [{
			type: 'index',
			name: '首页',
			separator: true
		}, {
			type: 'categories',
			name: '目录'
		}, {
			type: 'mine',
			name: '个人中心',
			showDot: toRef(global.states.curOperator, 'unread_notice_cnt'),
			separator: true
		}, {
			type: 'post',
			id: '402',
			name: '关于本站'
		}, {
			type: 'post',
			id: '427',
			name: '友情链接'
		}];
		return {
			menuItems
		};
	},
	created() {
		global.states.categoryManager.get({
			itemId: lpt.categoryServ.rootCategoryId,
			filter: res => res.sub_list,
			success: (result) => {
				const categoryItems = result.sub_list
					// .filter(item => item.type !== lpt.categoryServ.type.private)
					.map((category) => {
					return {
						type: 'category',
						id: category.id,
						name: category.name
					}
				});
				// 最后一个目录添加分割线
				categoryItems[categoryItems.length - 1].separator = true;
				this.menuItems.splice(1, 0, ...categoryItems);
			}
		});
	},
	methods: {
		onMenuItemMouseEnter(event, menuItem) {
			const menuItemElem = event.target;
			const menuBar = menuItemElem.getElementsByClassName('menu-bar')[0];
			const menuTitle = menuItemElem.getElementsByClassName('menu-title')[0];
			const menuName = menuTitle.getElementsByClassName('menu-name')[0];
			const offset = remHelper.remToPx(0.5);
			const menuNameWidth = menuName.getBoundingClientRect().width;
			menuName.style.color = 'rgba(255, 255, 255, 0.9)';
			menuBar.style.width = menuNameWidth + offset * 2 + 'px';
			if (menuItem.separator) {
				const menuSeparator = menuItemElem.getElementsByClassName('menu-separator')[0];
				menuSeparator.style.display = 'none';
			}
		},
		onMenuItemMouseLeave(event, menuItem) {
			const menuItemElem = event.target;
			const menuBar = menuItemElem.getElementsByClassName('menu-bar')[0];
			const menuTitle = menuItemElem.getElementsByClassName('menu-title')[0];
			const menuName = menuTitle.getElementsByClassName('menu-name')[0];
			menuBar.style.width = '0';
			menuName.style.color = 'rgba(255, 255, 255, 0.5)';
			if (menuItem.separator) {
				const menuSeparator = menuItemElem.getElementsByClassName('menu-separator')[0];
				menuSeparator.style.display = 'block';
			}
		},
		onMenuItemClick(menuItem) {
			let path;
			if (menuItem.type === 'index') {
				path = '/blog/index/' + lpt.categoryServ.rootCategoryId;
			} else if (menuItem.type === 'gallery') {
				path = '/blog/category_gallery_detail/' + blogDescription.galleryCategoryId;
			} else if (menuItem.type === 'categories') {
				path = '/categories';
			} else if (menuItem.type === 'mine') {
				path = '/home/mine';
			} else if (menuItem.type === 'post') {
				path = '/blog/post_detail/' + menuItem.id;
			} else if (menuItem.type === 'category') {
				path = '/blog/category_detail/' + menuItem.id;
			}
			this.$router.push({
				path: path
			});
		}
	},
};
</script>

<style scoped>
#menu-container {
	position: absolute;
	right: 0;
	top: 5%;
	width: 9%;
	height: 92.5%;
	padding-right: 0.5rem;
	overflow-y: scroll;
}

.menu-item {
	margin-bottom: 1.5rem;
}

.menu-title {
	font-weight: bold;
	text-align: right;
	width: auto;
	margin: 0;
}

.menu-name {
	font-size: 1.25rem;
	color: rgba(255, 255, 255, 0.5);
	transition: color 0.3s cubic-bezier(0.7, 0.3, 0.1, 1);
}

.menu-bar {
	position: absolute;
	height: 0.5rem;
	width: 0;
	right: 0;
	background-color: rgb(100, 120, 255);
	margin-top: 0.25rem;
	transition: width 0.5s cubic-bezier(0.7, 0.3, 0.1, 1),
		background-color 0.5s cubic-bezier(0.7, 0.3, 0.1, 1);
}

.menu-separator {
	position: absolute;
	height: 0.1rem;
	width: 100%;
	margin-top: 0.75rem;
	background-color: rgba(255, 255, 255, 0.1);
}
</style>