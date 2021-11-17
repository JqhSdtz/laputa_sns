<template>
	<div id="menu-container">
		<div id="menu-container-inner">
			<div class="menu-item" v-for="menuItem in menuItems" 
				:key="menuItem.type + (menuItem.id || '')"
				@mouseenter="onMenuItemMouseEnter($event, menuItem)"
				@mouseleave="onMenuItemMouseLeave($event, menuItem)"
				@click="onMenuItemClick(menuItem)">
				<p class="menu-title">
					<span class="menu-name" v-text="menuItem.name"></span>
				</p>
				<div class="menu-bar"></div>
				<div v-if="menuItem.separator" class="menu-separator"></div>
			</div>
		</div>
	</div>
</template>

<script>
import global from '@/lib/js/global';
import remHelper from '@/lib/js/uitls/rem-helper';
import lpt from '@/lib/js/laputa/laputa';

export default {
	name: 'FixedMenu',
	components: {},
	data() {
		const menuItems = [{
			type: 'index',
			name: '首页',
			separator: true
		}, {
			type: 'gallery',
			name: '相册'
		}, {
			type: 'categories',
			name: '目录'
		}, {
			type: 'mine',
			name: '个人中心',
			separator: true
		}, {
			type: 'post',
			id: '402',
			name: '网站介绍'
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
					.filter(item => item.type !== lpt.categoryServ.type.private)
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
			const fontSizeLarge = 1.5;
			// 因为有动画时间，所以有可能是鼠标移出再移入，并且动画未执行完，这时的fontSize不是初始值，而是动画过程中的一个值
			const curFontSize = window.getComputedStyle(menuName, null).getPropertyValue('font-size');
			const fontTransRate = remHelper.remToPx(fontSizeLarge) / parseFloat(curFontSize);
			const menuNameWidth = menuName.getBoundingClientRect().width * fontTransRate;
			menuName.style.color = 'rgba(255, 255, 255, 0.9)';
			menuName.style.fontSize = fontSizeLarge + 'rem';
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
			const fontSizeNormal = 1.25;
			menuBar.style.width = '0';
			menuName.style.color = 'rgba(255, 255, 255, 0.5)';
			menuName.style.fontSize = fontSizeNormal + 'rem';
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
				path = '/blog/gallery';
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
	height: 95%;
	padding-right: 0.5rem;
	overflow-y: scroll;
}

.menu-item {
	margin-top: 1.5rem;
	height: 1.95rem;
}

.menu-item:first-child {
	margin-top: 0.5rem;
}

.menu-title {
	font-weight: bold;
	color: rgba(255, 255, 255, 0.5);
	text-align: right;
	width: auto;
	margin: 0;
}

.menu-name {
	font-size: 1.25rem;
	transition: color 0.3s cubic-bezier(0.7, 0.3, 0.1, 1),
		font-size 0.3s cubic-bezier(0.7, 0.3, 0.1, 1);
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