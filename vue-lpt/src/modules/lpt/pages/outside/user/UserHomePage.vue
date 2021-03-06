<template>
	<div id="main-area" :style="{height: scrollHeight, position: 'relative'}">
		<div ref="infoBar">
			<a-row style="padding-top: 0.5rem;">
				<a-col span="5" offset="2">
					<img class="ava" :src="myAvatarUrl"/>
				</a-col>
				<a-col span="9" offset="1">
					<p class="name" style="margin-bottom: 0; margin-top: 0.5rem">{{ user.nick_name }}</p>
					<p>ID:{{ user.id }}</p>
				</a-col>
				<a-col span="3">
					<a-button class="follow-btn" :type="user.followed_by_viewer ? 'default' : 'primary'"
							@click="changeFollow">
						{{user.followed_by_viewer ? '取消关注' : '关注'}}
					</a-button>
				</a-col>
			</a-row>
			<a-row justify="center">
				<a-col class="cnt-bar-item" span="7" @click="showFollowingList">
					关注:{{ user.following_cnt }}
				</a-col>
				<a-col class="cnt-bar-item" span="8" @click="showFollowersList">
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
		<post-list ref="postList" :post-of="'creator'" :creator-id="parseInt(userId)"
		           :top-post-id="user.top_post_id" keep-scroll-top/>
	</div>
</template>

<script>
import PostList from '@/components/post/post_list/PostList';
import global from '@/lib/js/global';
import lpt from "@/lib/js/laputa/laputa";
import {Toast} from "vant";

export default {
	name: 'UserHomePage',
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
	created() {
		this.lptConsumer = lpt.createConsumer();
		lpt.userServ.get({
			consumer: this.lptConsumer,
			param: {
				userId: this.user.id
			},
			success(result) {
				global.states.userManager.add(result.object);
			},
			fail(result) {
				Toast.fail(result.message);
			}
		})
	},
	methods: {
		showFollowersList() {
			this.$router.push({
				name: 'followersList',
				params: {
					userId: this.userId
				}
			});
		},
		showFollowingList() {
			this.$router.push({
				name: 'followingList',
				params: {
					userId: this.userId
				}
			});
		},
		changeFollow() {
			const ref = this;
			const isCancel = this.user.followed_by_viewer;
			const fun = isCancel ? lpt.followServ.unFollow : lpt.followServ.follow;
			fun({
				data: {
					type: 0,
					target_id: this.userId
				},
				success() {
					Toast.success(isCancel ? '取关成功' : '关注成功');
					ref.user.followers_cnt += isCancel ? -1 : 1;
					ref.user.followed_by_viewer = !isCancel;
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
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

.ava {
	border-radius: 100%;
	margin: 0.5rem 1rem;
	width: 3.5rem;
	height: 3.5rem;
}

.name, .follow-btn {
	margin-top: 1rem;
}

.name {
	font-size: 1.5rem;
}

.cnt-bar-item {
	font-size: 1.15rem;
	font-weight: bold;
	text-align: center;
}
</style>