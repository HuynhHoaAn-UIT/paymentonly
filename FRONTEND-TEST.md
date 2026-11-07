# Test frontend ShopSphere Payment Demo (Windows PowerShell)

The interface retains the original ShopSphere dark navbar, product-card and checkout layout, but is rewritten as static HTML/CSS/JS so it can run on S3 without React or backend services.

```powershell
cd D:\shopsphere-payment-demo\frontend
py -m http.server 5500
```

Open http://localhost:5500. Click **Add to Cart & Checkout**, then **Thanh toán bằng MoMo**, then **Giả lập MoMo xác nhận thành công**.

`config.js` defaults to `DEMO_MODE=true`, so no API is contacted. When the backend is ready, set `DEMO_MODE=false`, point `PAYMENT_API_BASE` to your API endpoint and enable CORS on Payment API for the frontend origin. Real payment success requires a verified MoMo IPN, not a frontend click. The sample uses method `MOMO` for live backend mode.

Files: `index.html`, `styles.css`, `config.js`, `app.js`.
