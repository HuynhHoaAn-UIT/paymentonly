package com.shopsphere.payment;
import java.time.Instant;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
@ConditionalOnProperty(name="app.role",havingValue="worker")
public class PaymentWorker {
  private static final Logger log=LoggerFactory.getLogger(PaymentWorker.class);
  private final PaymentRepo repository;
  public PaymentWorker(PaymentRepo repository){this.repository=repository;}
  @Scheduled(fixedDelayString="${app.poll-ms:2000}")
  @Transactional
  public void work(){
    repository.markExpired(Instant.now(),Payment.Status.PENDING,Payment.Status.EXPIRED);
    for(Payment p:repository.findTop50ByStatusOrderByCreatedAtAsc(Payment.Status.SCANNED)){
      if(repository.markSuccess(p.id,Instant.now(),Payment.Status.SCANNED,Payment.Status.SUCCEEDED)==1)
        log.info("SIMULATION payment SUCCESS id={} order={}",p.id,p.orderId);
    }
  }
}