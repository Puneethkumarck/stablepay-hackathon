package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.remittance.exception.InvalidRemittanceStateException;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;

@ExtendWith(MockitoExtension.class)
class UpdateRemittanceStatusHandlerTest {

    @Mock
    private RemittanceRepository remittanceRepository;

    @InjectMocks
    private UpdateRemittanceStatusHandler updateRemittanceStatusHandler;

    @Test
    void shouldUpdateStatusForValidTransition() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.INITIATED).build();
        var expectedUpdated = remittance.toBuilder().status(RemittanceStatus.ESCROWED).build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));
        given(remittanceRepository.save(expectedUpdated)).willReturn(expectedUpdated);

        // when
        updateRemittanceStatusHandler.handle(SOME_REMITTANCE_ID, RemittanceStatus.ESCROWED);

        // then
        then(remittanceRepository).should().save(expectedUpdated);
    }

    @Test
    void shouldThrowWhenRemittanceNotFound() {
        // given
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> updateRemittanceStatusHandler.handle(
                SOME_REMITTANCE_ID, RemittanceStatus.ESCROWED))
                .isInstanceOf(RemittanceNotFoundException.class)
                .hasMessageContaining("SP-0010")
                .hasMessageContaining(SOME_REMITTANCE_ID.toString());
    }

    @Test
    void shouldThrowWhenTransitionIsInvalid() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.DELIVERED).build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));

        // when / then
        assertThatThrownBy(() -> updateRemittanceStatusHandler.handle(
                SOME_REMITTANCE_ID, RemittanceStatus.INITIATED))
                .isInstanceOf(InvalidRemittanceStateException.class)
                .hasMessageContaining("SP-0016")
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("INITIATED");
    }
}
