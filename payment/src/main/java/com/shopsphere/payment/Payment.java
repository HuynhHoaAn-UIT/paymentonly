package com.shopsphere.payment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="payment_transactions",indexes={@Index(name="idx_status",columnList="status"),@Index(name="idx_token",columnList="tokenHash",unique=true)})
public class Payment {
 public enum Status {PENDING,SCANNED,SUCCEEDED,EXPIRED}
 @Id @Column(length=36) public String id;
 @Column(nullable=false,unique=true,length=128) public String idempotencyKey;
 @Column(nullable=false,length=128) public String orderId;
 @Column(nullable=false) public long amount;
 @Column(nullable=false,length=3) public String currency="VND";
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) public Status status=Status.PENDING;
 @Column(nullable=false,length=64) public String tokenHash;
 @Column(nullable=false) public Instant createdAt;
 @Column(nullable=false) public Instant expiresAt;
 public Instant scannedAt;
 public Instant completedAt;
 protected Payment(){}
 public Payment(String id,String key,String order,long amount,String hash,Instant now){
   this.id=id;this.idempotencyKey=key;this.orderId=order;this.amount=amount;this.tokenHash=hash;
   this.createdAt=now;this.expiresAt=now.plusSeconds(600);
 }
}