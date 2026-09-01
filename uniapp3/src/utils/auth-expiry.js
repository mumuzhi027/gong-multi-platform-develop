import { reactive } from 'vue'

export const authExpiry = reactive({
  visible: false,
  sequence: 0,
})

export function notifyAuthExpired() {
  authExpiry.sequence += 1
  authExpiry.visible = true
}

export function clearAuthExpired() {
  authExpiry.visible = false
}
