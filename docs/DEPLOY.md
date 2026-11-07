# Deployment runbook

## 1. Copy payment backend
Copy the original repository's entire `Payment_Service` folder into `payment/` here. Keep the original Dockerfile, pom.xml, src and config. Do not copy the original docker-compose with unrelated services or embedded credentials.

## 2. RDS
Create RDS MySQL **Multi-AZ DB instance deployment** (primary and standby), private subnets in 2 AZs, no public access. Configure a dedicated database and least-privilege user for the payment service. Security group DB allows TCP/3306 from ECS task security group only. Enable backups, enhanced monitoring if needed. Store credentials in AWS Secrets Manager. Do not use root or commit passwords.

## 3. ECS and ALB
Create ONE ECS cluster and ONE ECS service with desired count **1**. Task definition contains TWO containers based on the same payment image:
- `payment-api`: `APP_ROLE=api`, `SERVER_PORT=8084`, ALB target group -> port 8084; health check `/actuator/health`.
- `payment-worker`: `APP_ROLE=worker`; no inbound port; same RDS connection and MoMo configuration.

Both share `SPRING_DATASOURCE_URL=jdbc:mysql://<RDS-ENDPOINT>:3306/payment_db`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` from Secrets Manager, `PAYMENT_GATEWAY=momo`, `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY`, `MOMO_ENDPOINT`, `MOMO_REDIRECT_URL`, `MOMO_IPN_URL`. Use **one task**; API and worker remain separate containers. Configure task CPU/memory appropriately, CloudWatch awslogs for both, ECS task execution/task IAM roles, ECR image and log groups. Allow ECS to reach MoMo outbound (NAT gateway or appropriate egress path). Public ALB forwards API requests to API container.

For a low-cost demo, the task may be in a public subnet with assigned public IP and a restrictive security group allowing ingress only from ALB, but a private subnet with NAT is preferable. RDS must remain private. ALB security group accepts inbound 80/443; ECS SG accepts 8084 from ALB SG; RDS SG accepts 3306 from ECS SG.

## 4. S3 frontend
Create S3 bucket for static website, upload `frontend/index.html`, `frontend/app.js`, `frontend/config.js`, configure website index `index.html`. Edit `frontend/config.js` with public ALB origin. S3 website endpoint uses **HTTP only** and requires public-read website access; avoid storing any secrets in frontend. Configure backend CORS to permit only the S3 website origin (Spring Security/CORS if applicable) including headers `Content-Type`, `Idempotency-Key`, methods `GET,POST,OPTIONS`. An ALB does not itself provide full application CORS behavior. For public/production use, front it with HTTPS CDN (excluded by this demo's scope).

## 5. MoMo sandbox
Use sandbox partner credentials, sandbox endpoint, a public **HTTPS** `MOMO_IPN_URL` routing to backend's MoMo IPN endpoint. Set redirect URL to S3 website result page if supported by MoMo; redirect alone is NOT proof of payment. Verify actual backend controller mapping, signature and request DTO. Ensure the worker polls the database and the IPN updates payment status. Confirm the exact fields returned by backend's `POST /api/v1/payments` and `GET /api/v1/payments/{paymentId}` match `frontend/app.js`; adjust JS if necessary. If your original worker only supports mock, change gateway to mock for a dry run, but the frontend request method must then match backend's accepted mock method.

## 6. CloudWatch and Grafana
Enable ALB access logs (optional S3 destination), CloudWatch ALB metrics `RequestCount`, `TargetResponseTime`, `HTTPCode_Target_5XX_Count`, `HTTPCode_ELB_5XX_Count`; ECS `CPUUtilization`, `MemoryUtilization`, running tasks, and application logs in CloudWatch Logs. For per-task request/latency/error rate, instrument Spring Boot Actuator/Micrometer with metrics collection and traces; ALB metrics alone cannot provide per-task granularity. Grafana needs a data source (CloudWatch) and suitable IAM permissions. Correlate request/payment IDs in application logs; never log payment secrets.

## 7. RDS failover test
Record baseline: create payment, read status, observe CloudWatch logs, ALB latency/error metrics, DB connections. In RDS console select the **Multi-AZ DB instance** -> Actions -> Reboot -> Reboot with failover (when offered). Observe interruption, endpoint continuity, application connection pool reconnect, worker retry behavior, and recovery time. RDS failover does not guarantee zero errors. Do not run this on a production database. Avoid sending a second payment with a different Idempotency-Key during recovery. Record timestamps/screenshots.

## 8. Cost caution
RDS Multi-AZ, ALB, NAT gateway, ECS Fargate, public IPv4 and logs can incur meaningful hourly charges even for a demo. Delete after testing.
