// true: test frontend without Docker/API/MoMo. false: call the real Payment API.
window.DEMO_MODE = true;
// For real local API: http://localhost:8084 (CORS must be enabled on backend).
// When using a same-origin proxy, use an empty string.
window.PAYMENT_API_BASE = 'http://localhost:8084';
