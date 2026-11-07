package com.shopsphere.payment;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1/payments")
@ConditionalOnProperty(name="app.role",havingValue="api",matchIfMissing=true)
public class PaymentApi {
  private final PaymentRepo repository;
  private final SecureRandom random=new SecureRandom();
  private final String publicUrl;
  public PaymentApi(PaymentRepo repository,@Value("${app.public-url:http://localhost:8084}") String publicUrl) {
    this.repository=repository;this.publicUrl=publicUrl.endsWith("/")?publicUrl.substring(0,publicUrl.length()-1):publicUrl;
  }
  public record NewPayment(String orderId,Long amount,String currency,String method){}
  public record Created(String paymentId,String orderId,long amount,String currency,String status,String qrCodeUrl,Instant expiresAt){}
  public record Status(String paymentId,String orderId,long amount,String currency,String status,Instant expiresAt,Instant scannedAt,Instant completedAt){}
  static String digest(String input) {
    try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}
    catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
  }
  private String newToken(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
  @PostMapping
  @Transactional
  public ResponseEntity<Created> create(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody NewPayment req) {
    if(key==null||key.isBlank()||key.length()>128||req==null||req.orderId()==null||req.orderId().isBlank()||
       req.orderId().length()>128||req.amount()==null||req.amount()<1||req.amount()>100000000||
       !"VND".equals(req.currency())||!"QR_DEMO".equals(req.method()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Required: Idempotency-Key; orderId; amount; currency=VND; method=QR_DEMO");
    var existing=repository.findByIdempotencyKey(key);
    if(existing.isPresent()){
      Payment p=existing.get();
      if(!p.orderId.equals(req.orderId())||p.amount!=req.amount())throw new ResponseStatusException(HttpStatus.CONFLICT,"Key already used");
      // Scan token is deliberately not stored in plaintext or returned on an idempotent replay.
      return ResponseEntity.status(202).body(new Created(p.id,p.orderId,p.amount,p.currency,p.status.name(),null,p.expiresAt));
    }
    String token=newToken();
    Payment p=new Payment(UUID.randomUUID().toString(),key,req.orderId(),req.amount(),digest(token),Instant.now());
    repository.saveAndFlush(p);
    String qrUrl=publicUrl+"/api/v1/payments/"+p.id+"/qr?token="+token;
    return ResponseEntity.status(202).body(new Created(p.id,p.orderId,p.amount,p.currency,p.status.name(),qrUrl,p.expiresAt));
  }
  @GetMapping("/{id}")
  @Transactional(readOnly=true)
  public Status status(@PathVariable String id){
    Payment p=repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found"));
    return new Status(p.id,p.orderId,p.amount,p.currency,p.status.name(),p.expiresAt,p.scannedAt,p.completedAt);
  }
  @GetMapping(value="/{id}/qr",produces="image/svg+xml")
  @Transactional(readOnly=true)
  public ResponseEntity<String> qr(@PathVariable String id,@RequestParam String token){
    if(token.length()>128)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    Payment p=repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    if(!MessageDigest.isEqual(digest(token).getBytes(StandardCharsets.UTF_8),p.tokenHash.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    if(p.status==Payment.Status.EXPIRED || (p.status==Payment.Status.PENDING&&!p.expiresAt.isAfter(Instant.now())))
      throw new ResponseStatusException(HttpStatus.GONE,"Expired QR");
    String scanUrl=publicUrl+"/api/v1/payments/scan/"+token;
    try{
      BitMatrix matrix=new MultiFormatWriter().encode(scanUrl,BarcodeFormat.QR_CODE,300,300,Map.of(EncodeHintType.MARGIN,2));
      StringBuilder svg=new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 300 300'><rect width='300' height='300' fill='white'/>");
      for(int y=0;y<matrix.getHeight();y++)for(int x=0;x<matrix.getWidth();x++)if(matrix.get(x,y))
        svg.append("<rect x='").append(x).append("' y='").append(y).append("' width='1' height='1' fill='black'/>");
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(svg.append("</svg>").toString());
    }catch(Exception e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"QR failure");}
  }
  // EDUCATIONAL SIMULATION: opening URL counts as scan, NOT actual financial settlement.
  @GetMapping(value="/scan/{token}",produces=MediaType.TEXT_HTML_VALUE)
  @Transactional
  public ResponseEntity<String> scan(@PathVariable String token){
    if(token.length()>128)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    String hash=digest(token);
    Payment p=repository.findByTokenHash(hash).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Invalid QR"));
    if(p.status==Payment.Status.EXPIRED||(p.status==Payment.Status.PENDING&&!p.expiresAt.isAfter(Instant.now())))
      throw new ResponseStatusException(HttpStatus.GONE,"QR expired");
    if(p.status==Payment.Status.PENDING)repository.markScanned(hash,Instant.now(),Payment.Status.PENDING,Payment.Status.SCANNED);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
      "<!doctype html><html lang='vi'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>ShopSphere Demo</title></head>"+
      "<body style='font:18px system-ui;background:#f6f8fb;text-align:center;padding:50px'><h1>✅ Đã quét mã QR</h1>"+
      "<p>Giao dịch mô phỏng được gửi đến Payment Worker.</p><p>Không có tiền thật được chuyển.</p></body></html>");
  }
}