# ShopSphere Payment-only Demo (AWS)

Minimal **S3 static frontend + ALB + 1 ECS Fargate task (2 containers: API and Worker) + RDS MySQL Multi-AZ**. No CloudFront, Auth, Product, Cart, Order services.

## Source of payment backend

The original Java 17 Spring Boot backend is **not bundled** in this archive. Copy **the complete `Payment_Service/` directory** from https://github.com/PhuSon26/ShopSphere_E-Commerce/tree/main/Payment_Service into this repository as `payment/`. Do not copy other services or the original frontend. The original payment API has `POST /api/v1/payments`, `GET /api/v1/payments/{paymentId}`, `APP_ROLE=api|worker`, and `/actuator/health`. See `docs/DEPLOY.md`.

**Check the actual payment response DTO:** `frontend/app.js` assumes the returned identifier is `paymentId` or `id`, and status is `status`. Adapt to the actual DTO if needed. The original payment worker must be running or a pending payment will not advance.

## Files
- `frontend/index.html`: one product, pay button, status.
- `frontend/config.js`: set public ALB origin URL.
- `frontend/app.js`: create payment with Idempotency-Key, poll status, show MoMo checkout link.
- `docs/DEPLOY.md`: AWS setup, network, security, failover test and monitoring.

## Important limitations
- This is a **deployment scaffold, not a tested end-to-end MoMo integration**. MoMo sandbox partner credentials and public HTTPS callback URL are required.
- The frontend **opens the MoMo-hosted checkout**, where MoMo displays its payment UI/QR; it does not fabricate a QR or mark payment succeeded locally.
- S3 static website hosting is HTTP only; use it for a controlled lab, not production checkout. The public ALB may need an HTTPS listener with a real domain/certificate, depending on MoMo callback requirements. S3 HTTP -> HTTPS ALB is allowed by browsers but CORS must be configured in the API.
- One ECS task is not high availability. RDS Multi-AZ protects the DB tier only.
