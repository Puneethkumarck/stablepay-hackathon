package com.stablepay.infrastructure.temporal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TaskQueue {
    REMITTANCE_LIFECYCLE(Constants.TASK_QUEUE_REMITTANCE_LIFECYCLE);

    private final String name;

    public static class Constants {
        public static final String TASK_QUEUE_REMITTANCE_LIFECYCLE =
                "stablepay-remittance-lifecycle";
    }
}
