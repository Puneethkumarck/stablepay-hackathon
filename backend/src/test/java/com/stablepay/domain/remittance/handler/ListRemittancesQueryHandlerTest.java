package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.port.RemittanceRepository;

@ExtendWith(MockitoExtension.class)
class ListRemittancesQueryHandlerTest {

    @Mock
    private RemittanceRepository remittanceRepository;

    @InjectMocks
    private ListRemittancesQueryHandler listRemittancesQueryHandler;

    @Test
    void shouldReturnPaginatedRemittancesForSender() {
        // given
        var pageable = PageRequest.of(0, 20);
        var remittance1 = remittanceBuilder().build();
        var remittance2 = remittanceBuilder()
                .remittanceId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
                .build();

        var page = new PageImpl<>(List.of(remittance1, remittance2), pageable, 2);
        given(remittanceRepository.findBySenderId(SOME_SENDER_ID, pageable)).willReturn(page);

        // when
        var result = listRemittancesQueryHandler.handle(SOME_SENDER_ID, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().getFirst())
                .usingRecursiveComparison()
                .isEqualTo(remittance1);
    }

    @Test
    void shouldReturnEmptyPageWhenNoRemittances() {
        // given
        var pageable = PageRequest.of(0, 20);
        var emptyPage = new PageImpl<Remittance>(List.of(), pageable, 0);
        given(remittanceRepository.findBySenderId(SOME_SENDER_ID, pageable)).willReturn(emptyPage);

        // when
        var result = listRemittancesQueryHandler.handle(SOME_SENDER_ID, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
