// Paste into the console of gonggong.mumuzhi.cloud before your next login.
// Reports only comparisons and HTTP status. No credentials/tokens are printed,
// stored or sent by this script. It does not initiate a login or retry.
// Call __gongRestoreLoginDiagnostic() or refresh to remove the diagnostic.
(() => {
  if (window.__gongRestoreLoginDiagnostic) window.__gongRestoreLoginDiagnostic()
  const originalOpen = XMLHttpRequest.prototype.open
  const originalSend = XMLHttpRequest.prototype.send
  const targets = new WeakMap()
  function open(method, url, ...args) {
    const target = new URL(url, location.href)
    targets.set(this, method.toUpperCase() === 'POST' && target.origin === location.origin && target.pathname === '/api/login')
    return originalOpen.call(this, method, url, ...args)
  }
  function send(body) {
    if (targets.get(this)) {
      const fields = document.querySelectorAll('.form input')
      const form = typeof body === 'string' ? new URLSearchParams(body) : null
      const report = {
        '提交包含用户名和密码字段': Boolean(form?.has('username') && form?.has('password')),
        '提交用户名与输入框一致': fields.length === 2 && form?.get('username') === fields[0].value.trim(),
        '提交密码与输入框一致': fields.length === 2 && form?.get('password') === fields[1].value,
      }
      this.addEventListener('loadend', () => {
        report['HTTP状态码'] = this.status
        console.table(report)
      }, { once: true })
    }
    return originalSend.call(this, body)
  }
  XMLHttpRequest.prototype.open = open
  XMLHttpRequest.prototype.send = send
  window.__gongRestoreLoginDiagnostic = () => {
    if (XMLHttpRequest.prototype.open === open) XMLHttpRequest.prototype.open = originalOpen
    if (XMLHttpRequest.prototype.send === send) XMLHttpRequest.prototype.send = originalSend
    delete window.__gongRestoreLoginDiagnostic
  }
  console.info('登录对照已启用。正常登录一次即可，只会输出是否一致与状态码；刷新页面可移除。')
})()
