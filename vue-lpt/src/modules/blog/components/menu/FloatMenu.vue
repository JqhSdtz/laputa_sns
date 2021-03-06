<template>
	<div>
		<div v-show="isMenuShow">
			<menu-item class="item" ref="index" path="/home/index">
				<template v-slot:normal>
					<ri-home3-line class="icon icon-normal"/>
					<div class="text text-normal">首页</div>
				</template>
				<template v-slot:active>
					<ri-home3-fill class="icon icon-active"/>
					<div class="text text-active">首页</div>
				</template>
			</menu-item>
			<menu-item class="item" ref="community" path="/home/community">
				<template v-slot:normal>
					<ri-dashboard-line class="icon icon-normal"/>
					<div class="text text-normal">社区</div>
				</template>
				<template v-slot:active>
					<ri-dashboard-fill class="icon icon-active"/>
					<div class="text text-active">社区</div>
				</template>
			</menu-item>
			<menu-item class="item" ref="news" path="/home/news">
				<template v-slot:normal>
					<a-badge :count="unreadNewsCnt" :overflow-count="99" style="line-height: inherit">
						<ri-apps-line class="icon icon-normal"/>
						<div class="text text-normal">动态</div>
					</a-badge>
				</template>
				<template v-slot:active>
					<ri-apps-fill class="icon icon-active"/>
					<div class="text text-active">动态</div>
				</template>
			</menu-item>
			<menu-item class="item" ref="mine" path="/home/mine">
				<template v-slot:normal>
					<a-badge :count="unreadNoticeCnt" :overflow-count="99" style="line-height: inherit">
						<ri-user3-line class="icon icon-normal"/>
						<div class="text text-normal">我的</div>
					</a-badge>
				</template>
				<template v-slot:active>
					<ri-user3-fill class="icon icon-active"/>
					<div class="text text-active">我的</div>
				</template>
			</menu-item>
		</div>
		<div id="round-menu" ref="floatBall" v-draggable="dragOption">
			<div id="round-outer" ref="ballOuter">
				<div id="round-inner"></div>
			</div>
		</div>
	</div>
</template>

<script>
import MenuItem from './MenuItem';
import {
	RiHome3Line, RiAppsLine, RiDashboardLine, RiUser3Line,
	RiHome3Fill, RiAppsFill, RiDashboardFill, RiUser3Fill
} from '@/assets/icons/remix-icon';
import global from '@/lib/js/global';
import {toRef} from "vue";

export default {
	name: 'FloatMenu',
	components: {
		MenuItem,
		RiHome3Line,
		RiAppsLine,
		RiDashboardLine,
		RiUser3Line,
		RiHome3Fill,
		RiAppsFill,
		RiDashboardFill,
		RiUser3Fill
	},
	data() {
		return {
			isMenuShow: false,
			startDrag: false,
			dragOption: {
				click: this.showMenu,
				move: () => {
					this.isMenuShow = false;
				}
			},
			unreadNoticeCnt: toRef(global.states.curOperator, 'unread_notice_cnt'),
			unreadNewsCnt: toRef(global.states.curOperator, 'unread_news_cnt')
		}
	},
	methods: {
		showMenu() {
			const refNames = ['index', 'community', 'news', 'mine'];
			const floatBallElem = this.$refs.floatBall;
			const ballOuterElem = this.$refs.ballOuter;
			const ballX = floatBallElem.offsetLeft + ballOuterElem.offsetWidth / 2;
			const ballY = floatBallElem.offsetTop + ballOuterElem.offsetHeight / 2;
			this.isMenuShow = true;
			this.dragOption.click = () => {
				this.isMenuShow = false;
				this.dragOption.click = this.showMenu;
			};
			const radius = 200;
			// 四个菜单项，180度被分成五份，第一个菜单角度为间隔角度
			const degreeDiff = Math.PI / (refNames.length + 1);
			let curDegree = degreeDiff;
			this.$nextTick(() => {
				refNames.forEach(name => {
					const elem = this.$refs[name].$el;
					let offsetX = Math.sin(curDegree) * radius;
					let offsetY = Math.cos(curDegree) * radius;
					// 额外减去50，让菜单和悬浮球靠的更近些
					offsetX += elem.offsetWidth / 2 - 50;
					offsetY += elem.offsetHeight / 2;
					elem.style.left = ballX - offsetX + 'px';
					elem.style.top = ballY - offsetY + 'px';
					curDegree += degreeDiff;
				});
			});
		}
	}
}
</script>

<style scoped>
.item {
	position: fixed;
}

#round-menu {
	/* 不要使用translate实现悬浮球居中，
		否则会导致悬浮球位置计算错误 */
	position: fixed;
	top: 50%;
	right: 0;
}

#round-outer {
	height: 2.5rem;
	width: 2.5rem;
	border-radius: 2.5rem;
	background-color: black;
	opacity: 0.25;
	padding: 0.45rem;
}

#round-outer:hover {
	opacity: 0.65;
}

#round-inner {
	height: 1.6rem;
	width: 1.6rem;
	border-radius: 1.6rem;
	border: white solid 0.1rem;
}

.icon {
	margin-top: 0.25rem;
	font-size: 1.75rem;
}

.text {
	font-size: 0.9rem;
}

.icon-active {
	color: forestgreen;
}

.text-active {
	color: forestgreen;
}
</style>