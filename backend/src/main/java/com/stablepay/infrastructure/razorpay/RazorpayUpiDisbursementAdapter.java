package com.stablepay.infrastructure.razorpay;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.stablepay.domain.common.PiiMasking;
import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.model.DisbursementResult;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "stablepay.disbursement.provider", havingValue = "razorpay")
@RequiredArgsConstructor
public class RazorpayUpiDisbursementAdapter implements FiatDisbursementProvider {

    private static final long MIN_PAISE = 100L;
    private static final String IDEMPOTENCY_HEADER = "X-Payout-Idempotency";
    private static final int RATE_LIMIT = 429;
    private static final int REQUEST_TIMEOUT = 408;

    private final RestClient razorpayRestClient;
    private final RazorpayDisbursementProperties properties;

    @Override
    public DisbursementResult disburse(
            String upiId, BigDecimal amountUsdc, BigDecimal amountInr, String remittanceId) {
        var paise = toPaise(upiId, amountInr);
        if (paise < MIN_PAISE) {
            throw DisbursementException.nonRetriable(
                    upiId, "INR amount below minimum: ₹" + amountInr);
        }
        var contactId = createContact(upiId, remittanceId);
        var fundAccountId = createFundAccount(upiId, contactId);
        return createPayout(upiId, remittanceId, fundAccountId, paise);
    }

    private long toPaise(String upiId, BigDecimal amountInr) {
        try {
            return amountInr.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw DisbursementException.nonRetriable(
                    upiId, "INR amount precision exceeds paise granularity: " + amountInr);
        }
    }

    private String createContact(String upiId, String remittanceId) {
        var request = RazorpayContactRequest.forRemittance(remittanceId);
        var response = executeCall(
                "createContact",
                upiId,
                () -> razorpayRestClient.post()
                        .uri("/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(RazorpayContactResponse.class));
        return Optional.ofNullable(response)
                .map(RazorpayContactResponse::id)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> DisbursementException.nonRetriable(
                        upiId, "Razorpay createContact returned no id"));
    }

    private String createFundAccount(String upiId, String contactId) {
        var request = RazorpayFundAccountRequest.forVpa(contactId, upiId);
        var response = executeCall(
                "createFundAccount",
                upiId,
                () -> razorpayRestClient.post()
                        .uri("/fund_accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(RazorpayFundAccountResponse.class));
        return Optional.ofNullable(response)
                .map(RazorpayFundAccountResponse::id)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> DisbursementException.nonRetriable(
                        upiId, "Razorpay createFundAccount returned no id"));
    }

    private DisbursementResult createPayout(
            String upiId, String remittanceId, String fundAccountId, long paise) {
        var request = RazorpayPayoutRequest.forPayout(
                properties.accountNumber(), fundAccountId, paise, remittanceId);
        var response = executeCall(
                "createPayout",
                upiId,
                () -> razorpayRestClient.post()
                        .uri("/payouts")
                        .header(IDEMPOTENCY_HEADER, remittanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(RazorpayPayoutResponse.class));
        var payout = Optional.ofNullable(response)
                .orElseThrow(() -> DisbursementException.nonRetriable(
                        upiId, "Razorpay createPayout returned empty body"));
        if (payout.id() == null || payout.id().isBlank()) {
            throw DisbursementException.nonRetriable(upiId, "Razorpay createPayout returned no id");
        }
        log.info("Razorpay payout created remittanceId={} payoutId={} status={} upi={}",
                remittanceId, payout.id(), payout.status(), PiiMasking.maskUpi(upiId));
        return DisbursementResult.builder()
                .providerId(payout.id())
                .providerStatus(payout.status() == null ? "unknown" : payout.status())
                .build();
    }

    private <T> T executeCall(String operation, String upiId, Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            return handleClientError(operation, upiId, e);
        } catch (HttpServerErrorException e) {
            var reason = sanitize(describeError(e));
            log.warn("Razorpay {} server error upi={} status={} reason={}",
                    operation, PiiMasking.maskUpi(upiId), e.getStatusCode().value(), reason);
            throw DisbursementException.retriable(upiId, e);
        } catch (ResourceAccessException e) {
            log.warn("Razorpay {} I/O failure upi={} cause={}",
                    operation, PiiMasking.maskUpi(upiId), e.getMessage());
            throw DisbursementException.retriable(upiId, e);
        } catch (RestClientResponseException e) {
            log.warn("Razorpay {} unexpected status upi={} status={} — treating as retriable",
                    operation, PiiMasking.maskUpi(upiId), e.getStatusCode().value());
            throw DisbursementException.retriable(upiId, e);
        }
    }

    private <T> T handleClientError(String operation, String upiId, HttpClientErrorException e) {
        var status = e.getStatusCode();
        var reason = sanitize(describeError(e));
        if (isTransientClientStatus(status)) {
            log.warn("Razorpay {} transient client error upi={} status={} reason={}",
                    operation, PiiMasking.maskUpi(upiId), status.value(), reason);
            throw DisbursementException.retriable(upiId, e);
        }
        log.warn("Razorpay {} rejected upi={} status={} reason={}",
                operation, PiiMasking.maskUpi(upiId), status.value(), reason);
        throw DisbursementException.nonRetriable(upiId, operation + " " + reason);
    }

    private boolean isTransientClientStatus(HttpStatusCode status) {
        return status.value() == RATE_LIMIT || status.value() == REQUEST_TIMEOUT;
    }

    private String describeError(HttpClientErrorException e) {
        return extractErrorCode(e).orElseGet(() -> "HTTP " + e.getStatusCode().value());
    }

    private String describeError(HttpServerErrorException e) {
        return extractErrorCode(e).orElseGet(() -> "HTTP " + e.getStatusCode().value());
    }

    private Optional<String> extractErrorCode(RestClientResponseException e) {
        try {
            var body = e.getResponseBodyAs(RazorpayErrorResponse.class);
            return Optional.ofNullable(body)
                    .map(RazorpayErrorResponse::error)
                    .map(err -> Optional.ofNullable(err.code()).orElse("UNKNOWN")
                            + ": "
                            + Optional.ofNullable(err.description()).orElse("no description"));
        } catch (RuntimeException parseEx) {
            return Optional.empty();
        }
    }

    private String sanitize(String input) {
        return PiiMasking.maskUpiSubstrings(input);
    }
}
