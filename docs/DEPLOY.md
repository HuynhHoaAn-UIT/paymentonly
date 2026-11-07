# AWS deploy — QR simulation
1. RDS MySQL Multi-AZ DB instance in private subnets; DB named payment_db, dedicated user, Secrets Manager; TCP/3306 from ECS SG only.
2. ECS Fargate task: TWO containers built from the same image. Set APP_ROLE=api and APP_ROLE=worker. Both use SPRING_DATASOURCE_URL/USERNAME/PASSWORD pointing to RDS. Worker has no exposed public port.
3. ALB routes 8084 to API. Health check /actuator/health. PUBLIC_API_BASE_URL must be the externally reachable ALB URL so phone can open QR link.
4. S3 static website: upload ALL frontend/*. files, adjust frontend/config.js PAYMENT_API_BASE, set CORS_ALLOWED_ORIGINS exact S3 website origin.
5. Monitor CloudWatch logs, ECS CPU/memory, ALB latency/5XX, RDS events. Test RDS Reboot with failover (supported Multi-AZ instance) and observe eventual recovery.
6. Educational QR simulation, no financial gateway. Scanning is implemented as **opening the URL**. It can be triggered by link preview crawlers; do not use as real payment proof.
7. Destroy resources after demo to avoid charges.
