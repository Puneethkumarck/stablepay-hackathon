package com.stablepay.infrastructure.transak;

record TransakOrderResponse(String orderId, String status, String depositAddress) {}
