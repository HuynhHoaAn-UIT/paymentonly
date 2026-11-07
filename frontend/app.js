const $ = id => document.getElementById(id);
const apiBase = (window.PAYMENT_API_BASE || '').replace(/\/$/, '');
const mock = window.DEMO_MODE === true;
let paymentId = null;
let timer = null;
let currentOrderId = null;

function showPage(name) {
  $('products-page').hidden = name !== 'products';
  $('checkout-page').hidden = name !== 'checkout';
  location.hash = name;
  window.scrollTo(0, 0);
}
function setStatus(message) { $('status').textContent = message; }
function resetCheckout() {
  clearTimeout(timer); paymentId = null; currentOrderId = null;
  $('pay').disabled = false; $('pay').textContent = 'Thanh toán bằng MoMo';
  $('checkout').replaceChildren(); $('mock-controls').hidden = true;
  $('note').textContent = mock ? 'Đang ở chế độ mô phỏng giao diện; chưa kết nối MoMo.' : 'Đang kết nối Payment API.';
  setStatus('Chưa thanh toán');
}
$('buy-btn').addEventListener('click', () => { resetCheckout(); $('cart-count').textContent = '1'; showPage('checkout'); });
$('back-btn').addEventListener('click', () => { clearTimeout(timer); $('cart-count').textContent = '0'; showPage('products'); });
$('mock-success').addEventListener('click', () => { $('mock-controls').hidden = true; setStatus('✅ Thanh toán thành công (MOCK UI)'); $('pay').disabled = false; $('pay').textContent = 'Tạo giao dịch mới'; });

async function parseResponse(response) {
  const body = await response.text();
  let data; try { data = JSON.parse(body); } catch { throw new Error(body || `HTTP ${response.status}`); }
  if (!response.ok) throw new Error(data.message || JSON.stringify(data));
  return data;
}
function addCheckoutLink(url) {
  if (!url || $('checkout').querySelector('a')) return;
  try { if (new URL(url).protocol !== 'https:') throw new Error('Unsafe checkout URL'); }
  catch { $('note').textContent = 'Checkout URL không hợp lệ hoặc không phải HTTPS.'; return; }
  const a = document.createElement('a'); a.href = url; a.target = '_blank'; a.rel = 'noopener noreferrer';
  a.textContent = 'Mở trang thanh toán MoMo ↗'; $('checkout').appendChild(a);
}
async function pollPayment(id) {
  const p = await parseResponse(await fetch(`${apiBase}/api/v1/payments/${encodeURIComponent(id)}`));
  const state = p.status || 'PENDING';
  if (state === 'SUCCEEDED') { setStatus('✅ Thanh toán thành công'); $('pay').disabled = false; $('pay').textContent = 'Tạo giao dịch mới'; return; }
  if (state === 'FAILED') { setStatus('❌ Thanh toán thất bại'); $('pay').disabled = false; return; }
  setStatus(`⏳ ${state}`); addCheckoutLink(p.checkoutUrl);
  timer = setTimeout(() => pollPayment(id).catch(e => { setStatus('Lỗi kiểm tra trạng thái'); $('note').textContent = e.message; }), 3000);
}
$('pay').addEventListener('click', async () => {
  resetCheckout(); $('pay').disabled = true;
  if (mock) {
    setStatus('⏳ Đang tạo giao dịch...');
    timer = setTimeout(() => { setStatus('⏳ Chờ thanh toán MoMo (giả lập)'); $('mock-controls').hidden = false; $('note').textContent = 'Nhấn nút giả lập để chuyển sang thành công. Không có tiền thật hay API nào được gọi.'; }, 700);
    return;
  }
  try {
    setStatus('⏳ Đang tạo giao dịch...');
    currentOrderId = `demo-${Date.now()}`;
    const response = await fetch(`${apiBase}/api/v1/payments`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', 'Idempotency-Key': `${currentOrderId}-payment-1` },
      body: JSON.stringify({ orderId: currentOrderId, amount: 125000, currency: 'VND', method: 'MOMO' })
    });
    const payment = await parseResponse(response);
    paymentId = payment.paymentId || payment.id;
    if (!paymentId) throw new Error('API không trả về paymentId/id');
    await pollPayment(paymentId);
  } catch (error) { setStatus('❌ Không thể tạo giao dịch'); $('note').textContent = error.message; $('pay').disabled = false; }
});
resetCheckout(); showPage(location.hash === '#checkout' ? 'checkout' : 'products');
