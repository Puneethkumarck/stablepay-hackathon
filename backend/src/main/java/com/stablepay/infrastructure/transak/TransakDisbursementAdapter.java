package com.stablepay.infrastructure.transak;

import java.math.BigDecimal;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import com.stablepay.domain.common.PiiMasking;
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
    public void disburse(String upiId, BigDecimal amountUsdc, String remittanceId) {
        log.info("Initiating Transak off-ramp: {} USDC to UPI {} for remittance {}",
                amountUsdc, PiiMasking.maskUpi(upiId), remittanceId);
        try {
            var quoteResponse = createQuote(amountUsdc);
            if (quoteResponse == null || quoteResponse.quoteId() == null || quoteResponse.quoteId().isBlank()) {
                throw DisbursementException.forRecipient(upiId, "Empty or invalid quote response from Transak");
            }
            log.info("Transak quote received for remittance {}: quoteId={}", remittanceId, quoteResponse.quoteId());

            var orderResponse = createOrder(quoteResponse.quoteId(), upiId, remittanceId);
            if (orderResponse == null || orderResponse.orderId() == null || orderResponse.orderId().isBlank()) {
                throw DisbursementException.forRecipient(upiId, "Empty or invalid order response from Transak");
            }
            log.info("Transak order created for remittance {}: orderId={}", remittanceId, orderResponse.orderId());
        } catch (DisbursementException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            log.error("Transak off-ramp failed for remittance {} with HTTP {}",
                    remittanceId, ex.getStatusCode());
            throw DisbursementException.forRecipient(upiId,
                    "Transak API returned HTTP " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Transak off-ramp failed for remittance {}", remittanceId);
            throw DisbursementException.forRecipient(upiId, "Transak service unavailable");
        }
    }

    private TransakQuoteResponse createQuote(BigDecimal amountUsdc) {
        return transakRestClient.post()
                .uri("/api/v1/partners/quotes")
                .body(TransakQuoteRequest.builder()
                        .cryptoCurrency("USDC")
                        .fiatCurrency("INR")
                        .fiatAmount(amountUsdc.toPlainString())
                        .type("SELL")
                        .build())
                .retrieve()
                .body(TransakQuoteResponse.class);
    }

    private TransakOrderResponse createOrder(String quoteId, String upiId, String remittanceId) {
        return transakRestClient.post()
                .uri("/api/v1/partners/orders")
                .body(TransakOrderRequest.builder()
                        .quoteId(quoteId)
                        .paymentDetails(upiId)
                        .partnerOrderId(remittanceId)
                        .build())
                .retrieve()
                .body(TransakOrderResponse.class);
    }
}
