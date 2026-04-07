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
    public void disburse(String upiId, String amountUsdc, String remittanceId) {
        log.info("Initiating Transak off-ramp: {} USDC to UPI {} for remittance {}",
                amountUsdc, maskUpi(upiId), remittanceId);
        try {
            var quoteResponse = createQuote(amountUsdc);
            if (quoteResponse == null) {
                throw DisbursementException.forRecipient(upiId, "Empty quote response from Transak");
            }
            log.info("Transak quote received for remittance {}: quoteId={}", remittanceId, quoteResponse.quoteId());

            var orderResponse = createOrder(quoteResponse.quoteId(), upiId, remittanceId);
            if (orderResponse == null) {
                throw DisbursementException.forRecipient(upiId, "Empty order response from Transak");
            }
            log.info("Transak order created for remittance {}: orderId={}", remittanceId, orderResponse.orderId());
        } catch (DisbursementException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Transak off-ramp failed for remittance {}: {}", remittanceId, ex.getMessage());
            throw DisbursementException.forRecipient(upiId, ex);
        }
    }

    private TransakQuoteResponse createQuote(String amountUsdc) {
        return transakRestClient.post()
                .uri("/api/v1/partners/quotes")
                .body(new TransakQuoteRequest("USDC", "INR", amountUsdc, "SELL"))
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
}
