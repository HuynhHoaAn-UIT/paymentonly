# ShopSphere QR Scan-to-Success Payment Demo

**Educational simulation — no money moves.** Based on the existing ShopSphere product + checkout frontend. No MoMo/VNPay or other gateway. Opening a QR link changes the payment to SCANNED, then the separate Payment Worker changes it to SUCCEEDED in MySQL.

## Architecture
- `frontend/` static HTML/CSS/JS (S3 website compatible)
- `payment-api`: Spring Boot Java 17, creates payment and QR SVG, accepts scan, exposes status
- `payment-worker`: same image with `APP_ROLE=worker`, polls MySQL SCANNED -> SUCCEEDED
- MySQL locally / RDS MySQL Multi-AZ on AWS
- One ECS cluster, one ECS service, one Fargate task with two containers

## Quick start (Windows PowerShell)
```powershell
git clone https://github.com/HuynhHoaAn-UIT/paymentonly.git
cd paymentonly
Copy-Item .env.example .env
notepad .env
docker compose up -d --build
docker compose ps
curl.exe http://localhost:8084/actuator/health
cd frontend
py -m http.server 5500
```
Open http://localhost:5500, choose mouse, Checkout, Tạo mã QR. On phone **open** scanned URL. Desktop polls and shows success.

**Phone networking:** `PUBLIC_API_BASE_URL=http://localhost:8084` does not work on a different phone (localhost refers to the phone). For phones on same Wi-Fi change `PUBLIC_API_BASE_URL` in `.env` to `http://YOUR_PC_LAN_IP:8084`, allow TCP 8084 from LAN only in Windows Firewall, then `docker compose up -d --force-recreate payment-api payment-worker`. The desktop frontend `PAYMENT_API_BASE` can remain localhost. For remote scanning use HTTPS ALB or a controlled HTTPS tunnel.

## API
POST `/api/v1/payments`, header `Idempotency-Key`, JSON:
```json
{"orderId":"demo-1001","amount":125000,"currency":"VND","method":"QR_DEMO"}
```
Returns HTTP 202 with `paymentId`, `status`, `qrCodeUrl`, `expiresAt`.
GET `/api/v1/payments/{id}` returns PENDING/SCANNED/SUCCEEDED/EXPIRED.
GET QR SVG `/{id}/qr?token=...`; embedded QR URL `/scan/{token}`.

## Build two tags from one Dockerfile
```powershell
docker build -t hoaaanhuynh/nt533:api -t hoaaanhuynh/nt533:worker ./payment
docker push hoaaanhuynh/nt533:api
docker push hoaaanhuynh/nt533:worker
```
The API and Worker are **two separate containers** running the same image with different `APP_ROLE` variables.

## Important
**Scanning/opening a URL is NOT proof of a real transfer.** Link preview bots can auto-open QR URLs. Do not use this implementation for accepting money or fulfilling paid orders. QR is valid for 10 minutes. Idempotent POST replays return the original transaction but do not re-expose the token. `ddl-auto=update` is only for lab. AWS RDS Multi-AZ provides DB failover, not uninterrupted request processing. See `docs/DEPLOY.md`.
