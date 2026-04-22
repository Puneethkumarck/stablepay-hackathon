package com.stablepay.application.controller.remittance;

import static com.stablepay.testutil.AuthFixtures.SOME_OTHER_USER_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_DB_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static com.stablepay.testutil.SecurityTestBase.asUser;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.controller.remittance.mapper.RemittanceApiMapper;
import com.stablepay.application.controller.remittance.mapper.RemittanceApiMapperImpl;
import com.stablepay.application.dto.CreateRemittanceRequest;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.handler.CreateRemittanceHandler;
import com.stablepay.domain.remittance.handler.GetRemittanceQueryHandler;
import com.stablepay.domain.remittance.handler.ListRemittancesQueryHandler;
import com.stablepay.domain.wallet.exception.InsufficientBalanceException;
import com.stablepay.testutil.TestClockConfig;
import com.stablepay.testutil.TestSecurityConfig;

import lombok.SneakyThrows;

@WebMvcTest(RemittanceController.class)
@Import({TestClockConfig.class, TestSecurityConfig.class, RemittanceApiMapperImpl.class})
class RemittanceControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateRemittanceHandler createRemittanceHandler;

    @MockitoBean
    private GetRemittanceQueryHandler getRemittanceQueryHandler;

    @MockitoBean
    private ListRemittancesQueryHandler listRemittancesQueryHandler;

    @MockitoSpyBean
    private RemittanceApiMapper remittanceApiMapper;

    @Test
    @SneakyThrows
    void shouldCreateRemittance() {
        // given
        var domain = remittanceBuilder().build();

        given(createRemittanceHandler.handle(SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC))
                .willReturn(domain);

        var request = CreateRemittanceRequest.builder()
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build();

        // when / then
        mockMvc.perform(post("/api/remittances")
                        .with(asUser(SOME_SENDER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SOME_REMITTANCE_DB_ID))
                .andExpect(jsonPath("$.remittanceId").value(SOME_REMITTANCE_ID.toString()))
                .andExpect(jsonPath("$.recipientPhone").value(SOME_RECIPIENT_PHONE))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @SneakyThrows
    void shouldGetRemittanceById() {
        // given
        var domain = remittanceBuilder().build();

        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID)).willReturn(domain);

        // when / then
        mockMvc.perform(get("/api/remittances/{remittanceId}", SOME_REMITTANCE_ID)
                        .with(asUser(SOME_SENDER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remittanceId").value(SOME_REMITTANCE_ID.toString()));
    }

    @Test
    @SneakyThrows
    void shouldListRemittancesByAuthenticatedUser() {
        // given
        var pageable = PageRequest.of(0, 20);
        var domain = remittanceBuilder().build();
        var page = new PageImpl<>(List.of(domain), pageable, 1);

        given(listRemittancesQueryHandler.handle(SOME_SENDER_ID, pageable)).willReturn(page);

        // when / then
        mockMvc.perform(get("/api/remittances")
                        .with(asUser(SOME_SENDER_ID))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].remittanceId").value(SOME_REMITTANCE_ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenRemittanceNotFound() {
        // given
        var unknownId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        given(getRemittanceQueryHandler.handle(unknownId, SOME_SENDER_ID))
                .willThrow(RemittanceNotFoundException.byId(unknownId));

        // when / then
        mockMvc.perform(get("/api/remittances/{remittanceId}", unknownId)
                        .with(asUser(SOME_SENDER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0010"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenInsufficientBalance() {
        // given
        given(createRemittanceHandler.handle(SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC))
                .willThrow(InsufficientBalanceException.forAmount(SOME_AMOUNT_USDC, BigDecimal.valueOf(50)));

        var request = CreateRemittanceRequest.builder()
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build();

        // when / then
        mockMvc.perform(post("/api/remittances")
                        .with(asUser(SOME_SENDER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0002"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenValidationFails() {
        // given
        var request = CreateRemittanceRequest.builder()
                .recipientPhone("")
                .amountUsdc(null)
                .build();

        // when / then
        mockMvc.perform(post("/api/remittances")
                        .with(asUser(SOME_SENDER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }

    @ParameterizedTest
    @MethodSource("authenticatedEndpoints")
    @SneakyThrows
    void shouldReturn401WhenNoBearer(String method, String path) {
        // given
        var requestBuilder = "POST".equals(method) ? post(path) : get(path);

        // when / then
        mockMvc.perform(requestBuilder)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SP-0040"));
    }

    static Stream<Arguments> authenticatedEndpoints() {
        return Stream.of(
                Arguments.of("POST", "/api/remittances"),
                Arguments.of("GET", "/api/remittances/" + SOME_REMITTANCE_ID),
                Arguments.of("GET", "/api/remittances"));
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenRemittanceBelongsToDifferentUser() {
        // given
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_OTHER_USER_ID))
                .willThrow(RemittanceNotFoundException.byId(SOME_REMITTANCE_ID));

        // when / then
        mockMvc.perform(get("/api/remittances/{remittanceId}", SOME_REMITTANCE_ID)
                        .with(asUser(SOME_OTHER_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SP-0010"));
    }
}
