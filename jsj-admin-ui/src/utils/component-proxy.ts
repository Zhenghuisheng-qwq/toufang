import { getCurrentInstance } from 'vue'

/** Only call synchronously from setup, after app plugins have been installed. */
export function useComponentProxy() {
  const proxy = getCurrentInstance()?.proxy
  if (!proxy) throw new Error('Component proxy is unavailable outside setup')
  return proxy
}
