const $ = id => document.getElementById(id);
const base = (window.PAYMENT_API_BASE || '').replace(/\/$/, '');
let timer = null;
let generation = 0;

function page(which){
  $('products-page').hidden = which !== 'products';
  $('checkout-page').hidden = which !== 'checkout';
  location.hash = which;
  window.scrollTo(0,0);
}
function status(message){$('status').textContent=message;}
function reset(){
  generation++;
  clearTimeout(timer);
  $('pay').disabled=false;
  $('pay').textContent='Tạo mã QR thanh toán';
  $('qr-area').hidden=true;
  $('payment-qr').removeAttribute('src');
  $('checkout').replaceChildren();
  $('note').textContent='Quét QR chỉ mô phỏng thanh toán, không chuyển tiền thật.';
  status('Chưa thanh toán');
}
$('buy-btn').addEventListener('click',()=>{
  reset(); $('cart-count').textContent='1'; page('checkout');
});
$('back-btn').addEventListener('click',()=>{
  reset(); $('cart-count').textContent='0'; page('products');
});
async function read(res){
  const body=await res.text();
  let data;
  try{data=JSON.parse(body);}catch{throw new Error(body||'Invalid API response');}
  if(!res.ok)throw new Error(data.detail||data.message||data.error||('HTTP '+res.status));
  return data;
}
async function poll(id,run){
  if(run!==generation)return;
  try{
    const p=await read(await fetch(base+'/api/v1/payments/'+encodeURIComponent(id),{cache:'no-store'}));
    if(run!==generation)return;
    if(p.status==='SUCCEEDED'){
      $('qr-area').hidden=true;
      status('✅ Thanh toán thành công (MÔ PHỎNG)');
      $('note').textContent='Worker đã cập nhật SUCCEEDED vào MySQL.';
      $('pay').disabled=false; $('pay').textContent='Tạo giao dịch mới';
      $('cart-count').textContent='0';
      return;
    }
    if(p.status==='EXPIRED'){
      $('qr-area').hidden=true;
      status('⌛ QR hết hạn');
      $('pay').disabled=false; $('pay').textContent='Tạo QR mới';
      return;
    }
    status(p.status==='SCANNED'?'📱 Đã quét QR, Worker đang xử lý...':'⏳ Chờ quét QR...');
    timer=setTimeout(()=>poll(id,run),2000);
  }catch(e){
    if(run!==generation)return;
    status('⚠️ Mất kết nối, đang thử lại...');
    $('note').textContent=e.message;
    timer=setTimeout(()=>poll(id,run),3000);
  }
}
$('pay').addEventListener('click',async()=>{
  reset();
  const run=generation;
  $('pay').disabled=true;
  status('⏳ Đang tạo giao dịch...');
  try{
    const key='demo-'+Date.now()+'-'+Math.random().toString(36).slice(2);
    const payment=await read(await fetch(base+'/api/v1/payments',{
      method:'POST',
      headers:{'Content-Type':'application/json','Idempotency-Key':key},
      body:JSON.stringify({orderId:key,amount:125000,currency:'VND',method:'QR_DEMO'})
    }));
    if(run!==generation)return;
    if(!payment.paymentId||!payment.qrCodeUrl)throw new Error('API thiếu QR; hãy tạo giao dịch mới');
    $('payment-qr').src=payment.qrCodeUrl;
    $('qr-expiry').textContent='Hết hạn: '+new Date(payment.expiresAt).toLocaleTimeString('vi-VN');
    $('qr-area').hidden=false;
    $('note').textContent='Mở URL trong QR trên điện thoại. Chỉ khi mở liên kết, API mới ghi nhận SCANNED.';
    await poll(payment.paymentId,run);
  }catch(e){
    if(run!==generation)return;
    $('pay').disabled=false;
    status('❌ Không tạo được QR');
    $('note').textContent=e.message;
  }
});
reset();
page(location.hash==='#checkout'?'checkout':'products');
