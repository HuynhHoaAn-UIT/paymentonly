package com.shopsphere.payment;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface PaymentRepo extends JpaRepository<Payment,String> {
 Optional<Payment> findByIdempotencyKey(String key);
 Optional<Payment> findByTokenHash(String tokenHash);
 List<Payment> findTop50ByStatusOrderByCreatedAtAsc(Payment.Status status);
 @Modifying(clearAutomatically=true,flushAutomatically=true)
 @Query("update Payment p set p.status = :scanned, p.scannedAt = :now where p.tokenHash = :hash and p.status = :pending and p.expiresAt > :now")
 int markScanned(@Param("hash") String hash,@Param("now") Instant now,@Param("pending") Payment.Status pending,@Param("scanned") Payment.Status scanned);
 @Modifying(clearAutomatically=true,flushAutomatically=true)
 @Query("update Payment p set p.status = :done, p.completedAt = :now where p.id = :id and p.status = :scanned")
 int markSuccess(@Param("id") String id,@Param("now") Instant now,@Param("scanned") Payment.Status scanned,@Param("done") Payment.Status done);
 @Modifying(clearAutomatically=true,flushAutomatically=true)
 @Query("update Payment p set p.status = :expired where p.status = :pending and p.expiresAt <= :now")
 int markExpired(@Param("now") Instant now,@Param("pending") Payment.Status pending,@Param("expired") Payment.Status expired);
}