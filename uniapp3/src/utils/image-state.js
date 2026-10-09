import { reactive } from 'vue'

// Key by the resolved URL rather than an activity ID: an edited poster must load
// successfully before receiving the white text and shade of the previous one.
export function useImageState() {
  const states = reactive(new Map())
  return {
    loaded: url => states.get(url) === 'loaded',
    failed: url => states.get(url) === 'failed',
    load: url => states.set(url, 'loaded'),
    error: url => states.set(url, 'failed'),
  }
}
