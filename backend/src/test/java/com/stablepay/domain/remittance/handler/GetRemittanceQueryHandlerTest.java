package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.port.RemittanceRepository;

@ExtendWith(MockitoExtension.class)
class GetRemittanceQueryHandlerTest {

    @Mock
    private RemittanceRepository remittanceRepository;

    @InjectMocks
    private GetRemittanceQueryHandler getRemittanceQueryHandler;

    @Test
    void shouldReturnRemittanceById() {
        // given
        var remittance = remittanceBuilder().build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));

        // when
        var result = getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(remittance);
    }

    @Test
    void shouldThrowWhenRemittanceNotFound() {
        // given
        var unknownId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        given(remittanceRepository.findByRemittanceId(unknownId))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> getRemittanceQueryHandler.handle(unknownId))
                .isInstanceOf(RemittanceNotFoundException.class)
                .hasMessageContaining("SP-0010")
                .hasMessageContaining(unknownId.toString());
    }
}
