// uni-app H5 throttles update:modelValue. At submission the native field can
// already show the new value while the Vue model still contains the old one.
export function currentInputValue(component, fallback) {
  // #ifdef H5
  const root = component?.$el || component
  const input = root?.matches?.('input') ? root : root?.querySelector?.('input')
  if (input && typeof input.value === 'string') return input.value
  // #endif
  return fallback
}
