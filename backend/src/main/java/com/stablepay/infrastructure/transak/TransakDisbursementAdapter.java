package com.stablepay.infrastructure.transak;

import org.springframework.web.client.RestClient;

import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TransakDisbursementAdapter implements FiatDisbursementProvider {

    private final RestClient transakRestClient;
    private final TransakProperties properties;

    @Override
    public void disburse(String upiId, String amountInr, String remittanceId) {
        log.info("Initiating Transak off-ramp: {} INR to UPI {} for remittance {}",
                amountInr, maskUpi(upiId), remittanceId);
        try {
            var quoteResponse = createQuote(amountInr);
            log.info("Transak quote received for remittance {}: quoteId={}", remittanceId, quoteResponse.quoteId());

            var orderResponse = createOrder(quoteResponse.quoteId(), upiId, remittanceId);
            log.info("Transak order created for remittance {}: orderId={}", remittanceId, orderResponse.orderId());
        } catch (DisbursementException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Transak off-ramp failed for remittance {}: {}", remittanceId, ex.getMessage());
            throw DisbursementException.forRecipient(upiId, ex);
        }
    }

    private TransakQuoteResponse createQuote(String amountInr) {
        return transakRestClient.post()
                .uri("/api/v1/partners/quotes")
                .body(new TransakQuoteRequest("USDC", "INR", amountInr, "SELL"))
                .retrieve()
                .body(TransakQuoteResponse.class);
    }

    private TransakOrderResponse createOrder(String quoteId, String upiId, String remittanceId) {
        return transakRestClient.post()
                .uri("/api/v1/partners/orders")
                .body(new TransakOrderRequest(quoteId, upiId, remittanceId))
                .retrieve()
                .body(TransakOrderResponse.class);
    }

    private static String maskUpi(String upiId) {
        if (upiId == null || upiId.length() <= 4) {
            return "****";
        }
        return upiId.substring(0, 3) + "****";
    }

    record TransakQuoteRequest(
        String cryptoCurrency,
        String fiatCurrency,
        String fiatAmount,
        String type
    ) {}

    record TransakQuoteResponse(String quoteId, String fiatAmount, String cryptoAmount) {}

    record TransakOrderRequest(String quoteId, String paymentDetails, String partnerOrderId) {}

    record TransakOrderResponse(String orderId, String status, String depositAddress) {}
}
