<script>
export default {
  onLaunch() {
    const savedTheme = uni.getStorageSync('gong_theme') || 'light'
    uni.setStorageSync('gong_theme', savedTheme)
  },
}
</script>

<style lang="scss">
@import './styles/global.scss';
</style>
