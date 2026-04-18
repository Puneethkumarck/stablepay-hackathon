package com.stablepay.infrastructure.temporal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TaskQueue {
    REMITTANCE_LIFECYCLE(Constants.TASK_QUEUE_REMITTANCE_LIFECYCLE),
    WALLET_FUNDING(Constants.TASK_QUEUE_WALLET_FUNDING);

    private final String name;

    public static class Constants {
        public static final String TASK_QUEUE_REMITTANCE_LIFECYCLE =
                "stablepay-remittance-lifecycle";
        public static final String TASK_QUEUE_WALLET_FUNDING =
                "stablepay-wallet-funding";
    }
}
