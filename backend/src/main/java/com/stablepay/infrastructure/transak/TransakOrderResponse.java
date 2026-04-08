package com.stablepay.infrastructure.transak;

import lombok.Builder;

@Builder(toBuilder = true)
record TransakOrderResponse(String orderId, String status, String depositAddress) {}
