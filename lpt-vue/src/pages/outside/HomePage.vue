<template>
	<div id="main-area" :style="{height: scrollHeight, position: 'relative'}">
		<div ref="infoBar">
			<a-row style="padding-top: 0.5rem;">
				<a-col span="5" offset="2">
					<img class="ava" :src="myAvatarUrl"/>
				</a-col>
				<a-col span="16" offset="1">
					<p class="name">{{ user.nick_name }}</p>
				</a-col>
			</a-row>
			<a-row justify="center">
				<a-col class="cnt-bar-item" span="7">
					关注:{{ user.following_cnt }}
				</a-col>
				<a-col class="cnt-bar-item" span="8">
					粉丝:{{ user.followers_cnt }}
				</a-col>
				<a-col class="cnt-bar-item" span="7">
					发帖:{{ user.post_cnt }}
				</a-col>
			</a-row>
			<van-divider style="margin: 5px 0"/>
			<div style="height: 5px; width: 100%; background-color: #ECECEC"/>
		</div>
		<a-back-top :style="{bottom: '30px'}" :target="getElement"/>
		<post-list ref="postList" :post-of="'creator'" :creator-id="parseInt(userId)" keep-scroll-top/>
	</div>
</template>

<script>
import PostList from '@/components/post/PostList';
import global from '@/lib/js/global';
import lpt from "@/lib/js/laputa/laputa";

export default {
	name: 'HomePage',
	components: {
		PostList
	},
	props: {
		userId: String
	},
	data() {
		const user = global.states.userManager.get(parseInt(this.userId));
		return {
			user,
			postListLoaded: false,
			showSortTypeSelector: false,
			showPopover: false
		}
	},
	computed: {
		scrollHeight() {
			return document.body.clientHeight + 'px';
		},
		myAvatarUrl() {
			return lpt.getUserAvatarUrl(this.user);
		}
	},
	methods: {
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

.ava {
	border-radius: 100%;
	margin: 0.5rem 1rem;
	width: 3.5rem;
	height: 3.5rem;
}

.name {
	margin-top: 1rem;
	font-size: 1.5rem;
}

.cnt-bar-item {
	font-size: 1.15rem;
	font-weight: bold;
	text-align: center;
}
</style>