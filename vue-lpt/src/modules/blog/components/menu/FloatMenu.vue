<template>
	<div>
		<div v-show="isMenuShown">
			<menu-item class="item" ref="index" name="">
				<ri-home3-fill class="icon"/>
				<div class="text">相册</div>
			</menu-item>
			<menu-item class="item" ref="community" name="">
				<ri-dashboard-fill class="icon"/>
				<div class="text">文章</div>
			</menu-item>
			<menu-item class="item" ref="news" name="">
				<ri-apps-fill class="icon"/>
				<div class="text">分类</div>
			</menu-item>
			<menu-item class="item" ref="mine" name="mine">
				<img class="ava" :class="{'def-ava': !me.user.raw_avatar}" :src="myAvatarUrl"/>
				<div class="text">{{ me.user.nick_name || '未登录' }}</div>
			</menu-item>
		</div>
		<div id="round-menu" ref="floatBall" v-draggable="dragOption">
			<div id="round-outer" :class="{inactive: !isMenuShown}" ref="ballOuter">
				<div v-show="isMenuShown">
					<ri-close-fill id="close-icon"/>
				</div>
				<div v-show="!isMenuShown" id="round-inner"></div>
			</div>
		</div>
	</div>
</template>

<script>
import MenuItem from './MenuItem';
import {
	RiHome3Fill, RiAppsFill, RiDashboardFill, RiCloseFill
} from '@/assets/icons/remix-icon';
import global from '@/lib/js/global';
import {toRef} from "vue";
import lpt from "@/lib/js/laputa/laputa";

export default {
	name: 'FloatMenu',
	components: {
		MenuItem,
		RiHome3Fill,
		RiAppsFill,
		RiDashboardFill,
		RiCloseFill
	},
	data() {
		return {
			me: global.states.curOperator,
			isMenuShown: false,
			startDrag: false,
			dragOption: {
				click: this.showMenu,
				move: () => {
					this.isMenuShown = false;
				}
			},
			unreadNoticeCnt: toRef(global.states.curOperator, 'unread_notice_cnt'),
			unreadNewsCnt: toRef(global.states.curOperator, 'unread_news_cnt')
		}
	},
	computed: {
		hasSigned() {
			return global.states.hasSigned.value;
		},
		myAvatarUrl() {
			return lpt.getUserAvatarUrl(this.me.user);
		}
	},
	methods: {
		showMenu() {
			// 这一句不能放到最后
			this.isMenuShown = true;
			const refNames = ['index', 'community', 'news', 'mine'];
			const floatBallElem = this.$refs.floatBall;
			const ballOuterElem = this.$refs.ballOuter;
			const ballX = floatBallElem.offsetLeft + ballOuterElem.offsetWidth / 2;
			const ballY = floatBallElem.offsetTop + ballOuterElem.offsetHeight / 2;
			this.dragOption.click = () => {
				this.isMenuShown = false;
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
	bottom: 50%;
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

#round-outer:not(.inactive) {
	opacity: 0.65;
}

#round-outer:not(.inactive):hover {
	opacity: 0.45;
}

#round-outer.inactive:hover {
	opacity: 0.65;
}

#round-inner {
	position: absolute;
	height: 1.6rem;
	width: 1.6rem;
	border-radius: 1.6rem;
	border: white solid 0.1rem;
}

#close-icon {
	position: absolute;
	color: white;
	height: 1.6rem;
	width: 1.6rem;
	font-size: 1.6rem;
}

.icon {
	margin-top: 0.25rem;
	font-size: 1.75rem;
}

.text {
	font-size: 0.9rem;
}

.ava {
	border-radius: 100%;
	margin: 0.5rem 1rem;
	width: 2rem;
	height: 2rem;
}

.def-ava {
	margin: 0.3rem 0.6rem;
	width: 2.2rem;
	height: 2.2rem;
}
</style>