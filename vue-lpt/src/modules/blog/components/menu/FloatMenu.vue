<template>
	<div ref="menuMain">
		<div v-show="isMenuShown" @click="closeMenu">
			<menu-item class="item" :with-background="true" ref="gallery" path="/blog/gallery">
				<ri-landscape-line class="icon"/>
				<p class="text">相册</p>
			</menu-item>
			<menu-item class="item" :with-background="true" ref="category" path="/categories" :show-in-drawer="true">
				<ri-dashboard-fill class="icon"/>
				<p class="text">目录</p>
			</menu-item>
			<menu-item class="item" :with-background="true" ref="index" :path="'/blog/index/' + rootCategoryId">
				<ri-apps-fill class="icon"/>
				<p class="text">首页</p>
			</menu-item>
			<menu-item class="item" ref="mine" path="/home/mine" :show-in-drawer="true">
				<img class="ava" :class="{'def-ava': !me.user.raw_avatar}" :src="myAvatarUrl"/>
				<p class="text user-name">{{ me.user.nick_name || '未登录' }}</p>
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
	RiLandscapeLine, RiAppsFill, RiDashboardFill, RiCloseFill
} from '@/assets/icons/remix-icon';
import global from '@/lib/js/global';
import {toRef} from 'vue';
import lpt from "@/lib/js/laputa/laputa";

export default {
	name: 'FloatMenu',
	components: {
		MenuItem,
		RiLandscapeLine,
		RiAppsFill,
		RiDashboardFill,
		RiCloseFill
	},
	data() {
		return {
			me: global.states.curOperator,
			rootCategoryId: lpt.categoryServ.rootCategoryId,
			isMenuShown: false,
			startDrag: false,
			dragOption: {
				click: this.showMenu,
				move: () => {
					this.closeMenu();
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
	created() {
		window.addEventListener('click', (event) => {
			if (!this.$refs.menuMain) return;
			if (!this.$refs.menuMain.contains(event.target)) {
				this.closeMenu();
			}
		});
	},
	methods: {
		closeMenu() {
			this.isMenuShown = false;
			this.dragOption.click = this.showMenu;
		},
		getBallPos() {
			const floatBallElem = this.$refs.floatBall;
			const ballOuterElem = this.$refs.ballOuter;
			const ballX = floatBallElem.offsetLeft + ballOuterElem.offsetWidth / 2;
			const ballY = floatBallElem.offsetTop + ballOuterElem.offsetHeight / 2;
			return {
				x: ballX,
				y: ballY
			}
		},
		showMenu() {
			// 这一句不能放到最后
			this.isMenuShown = true;
			const refNames = ['gallery', 'category', 'index', 'mine'];
			const ballPos = this.getBallPos();
			this.dragOption.click = this.closeMenu;
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
					elem.style.left = ballPos.x - offsetX + 'px';
					elem.style.top = ballPos.y - offsetY + 'px';
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
	height: 2.75rem;
	width: 2.75rem;
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
	position: absolute;
	left: 50%;
	top: 0.35rem;
	margin-left: 0.03rem;
	transform: translateX(-50%);
	font-size: 1.75rem;
}

.text {
	position: absolute;
	bottom: 0;
	width: 100%;
	margin: 0.25rem 0;
	text-align: center;
	font-size: 0.9rem;
}

.ava {
	border-radius: 100%;
	margin: 0.75rem 1.5rem;
	width: 3rem;
	height: 3rem;
	box-shadow: 0 0 10px 6px rgba(0, 0, 0, 0.25);
}

.user-name {
	position: relative;
	margin-top: 0;
	background-color: white;
	box-shadow: 0 0 10px 6px rgba(0, 0, 0, 0.25);
}
.def-ava {
	margin: 0.4rem 0.8rem;
	width: 3.2rem;
	height: 3.2rem;
}
</style>