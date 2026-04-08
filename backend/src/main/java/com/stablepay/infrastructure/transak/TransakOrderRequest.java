package com.stablepay.infrastructure.transak;

record TransakOrderRequest(String quoteId, String paymentDetails, String partnerOrderId) {}
