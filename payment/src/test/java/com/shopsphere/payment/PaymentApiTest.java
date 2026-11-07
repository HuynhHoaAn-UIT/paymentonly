package com.shopsphere.payment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class PaymentApiTest {
 @Test void rejectsInvalidRequest() {
  PaymentRepo repo=mock(PaymentRepo.class);
  PaymentApi api=new PaymentApi(repo,"http://localhost:8084");
  assertThrows(ResponseStatusException.class,()->api.create(null,new PaymentApi.NewPayment("one",125000L,"VND","QR_DEMO")));
  verifyNoInteractions(repo);
 }
 @Test void createsQrForValidRequest(){
  PaymentRepo repo=mock(PaymentRepo.class);
  when(repo.findByIdempotencyKey("once")).thenReturn(Optional.empty());
  when(repo.saveAndFlush(any())).thenAnswer(inv->inv.getArgument(0));
  PaymentApi api=new PaymentApi(repo,"http://localhost:8084");
  var response=api.create("once",new PaymentApi.NewPayment("one",125000L,"VND","QR_DEMO"));
  assertEquals(202,response.getStatusCode().value());
  assertEquals("PENDING",response.getBody().status());
  assertTrue(response.getBody().qrCodeUrl().contains("/qr?token="));
 }
}
