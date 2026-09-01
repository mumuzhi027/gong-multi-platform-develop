<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { WEB_BASE_URL } from '../../config/env'

const type = ref('userClause')
const title = ref('用户协议')

const fallbackContent = {
  privacyPolicy: {
    heading: '隐私政策',
    sections: [
      ['信息收集', '为提供课表、成绩、考试和空教室服务，应用可能处理学号、姓名、专业及教务数据。'],
      ['信息使用', '相关信息仅用于展示校园服务内容、维持登录状态和改善使用体验。'],
      ['本地存储', '为支持离线查看，部分课程与学业数据会保存在当前设备的小程序存储空间中。'],
      ['联系我们', '如对个人信息处理有疑问，请通过项目维护方提供的渠道联系我们。'],
    ],
  },
  permissionsRequest: {
    heading: '权限申请与使用情况说明',
    sections: [
      ['网络权限', '用于登录并请求课表、考试、成绩、空教室等服务数据。'],
      ['存储能力', '使用小程序本地存储保存登录凭证、偏好设置和离线缓存。'],
      ['订阅消息', '仅在你主动授权后，用于发送课程或考试提醒；你可以随时在微信设置中关闭。'],
    ],
  },
  userClause: {
    heading: '用户协议',
    sections: [
      ['服务说明', '拱拱是校园信息辅助工具，展示内容以学校教务系统和相关数据源为准。'],
      ['账号安全', '请妥善保管个人账号与设备，不要将登录凭证提供给他人。'],
      ['合理使用', '不得利用本服务干扰学校系统、批量抓取数据或从事违反法律法规的行为。'],
      ['变更与终止', '服务内容可能因学校系统、接口策略或小程序平台规则调整。'],
    ],
  },
}

onLoad((query) => {
  type.value = query.type || 'userClause'
  title.value = decodeURIComponent(query.title || fallbackContent[type.value]?.heading || '条款')
  uni.setNavigationBarTitle({ title: title.value })
})

function openOnline() {
  // 小程序 web-view 需要将域名配置为业务域名；默认提供内置可读版本。
  // #ifdef H5
  window.open(`${WEB_BASE_URL}#/${type.value}`, '_blank')
  // #endif
  // #ifndef H5
  uni.showToast({ title: '请先配置小程序业务域名', icon: 'none' })
  // #endif
}
</script>

<template>
  <view class="page clause-page">
    <view class="clause-card card">
      <view class="clause-title">{{ fallbackContent[type]?.heading || title }}</view>
      <view class="update-time">更新日期：2026年7月25日</view>
      <view v-for="section in fallbackContent[type]?.sections || []" :key="section[0]" class="section">
        <view class="section-heading">{{ section[0] }}</view>
        <view class="section-content">{{ section[1] }}</view>
      </view>
    </view>
    <button class="online-button" @tap="openOnline">查看线上完整版本</button>
  </view>
</template>

<style scoped lang="scss">
.clause-page { padding: 26rpx 24rpx 50rpx; }
.clause-card { padding: 40rpx 34rpx; }
.clause-title { font-size: 42rpx; font-weight: 900; }
.update-time { margin-top: 10rpx; color: #999; font-size: 20rpx; }
.section { margin-top: 38rpx; }
.section-heading { font-size: 29rpx; font-weight: 800; }
.section-content { margin-top: 14rpx; color: #555; font-size: 24rpx; line-height: 1.85; }
.online-button { margin-top: 30rpx; border-radius: 40rpx; color: #24617e; background: #dff2fd; font-size: 24rpx; line-height: 78rpx; }
</style>
