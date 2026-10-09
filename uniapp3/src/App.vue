<script>
// #ifdef H5
import { initPwa } from './pwa/client'
// #endif
export default {
  onLaunch() {
    const savedTheme = uni.getStorageSync('gong_theme') || 'light'
    uni.setStorageSync('gong_theme', savedTheme)
    // #ifdef H5
    initPwa()
    // #endif
  },
}
</script>

<style lang="scss">
@import './styles/global.scss';
</style>
