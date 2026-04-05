package com.stablepay.application.controller.remittance;

import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_DB_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_UPDATED_AT;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.application.controller.remittance.mapper.RemittanceApiMapper;
import com.stablepay.application.dto.CreateRemittanceRequest;
import com.stablepay.application.dto.RemittanceResponse;
import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.handler.CreateRemittanceHandler;
import com.stablepay.domain.remittance.handler.GetRemittanceQueryHandler;
import com.stablepay.domain.remittance.handler.ListRemittancesQueryHandler;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.wallet.exception.InsufficientBalanceException;

import lombok.SneakyThrows;

@WebMvcTest(RemittanceController.class)
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

    @MockitoBean
    private RemittanceApiMapper remittanceApiMapper;

    @Test
    @SneakyThrows
    void shouldCreateRemittance() {
        // given
        var domain = remittanceBuilder().build();
        var response = RemittanceResponse.builder()
                .id(SOME_REMITTANCE_DB_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        given(createRemittanceHandler.handle(SOME_SENDER_ID, SOME_RECIPIENT_PHONE, SOME_AMOUNT_USDC))
                .willReturn(domain);
        given(remittanceApiMapper.toResponse(domain)).willReturn(response);

        var request = CreateRemittanceRequest.builder()
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build();

        // when / then
        mockMvc.perform(post("/api/remittances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SOME_REMITTANCE_DB_ID))
                .andExpect(jsonPath("$.remittanceId").value(SOME_REMITTANCE_ID.toString()))
                .andExpect(jsonPath("$.senderId").value(SOME_SENDER_ID))
                .andExpect(jsonPath("$.recipientPhone").value(SOME_RECIPIENT_PHONE))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @SneakyThrows
    void shouldGetRemittanceById() {
        // given
        var domain = remittanceBuilder().build();
        var response = RemittanceResponse.builder()
                .id(SOME_REMITTANCE_DB_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.INITIATED)
                .smsNotificationFailed(false)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();

        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID)).willReturn(domain);
        given(remittanceApiMapper.toResponse(domain)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/remittances/{remittanceId}", SOME_REMITTANCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remittanceId").value(SOME_REMITTANCE_ID.toString()))
                .andExpect(jsonPath("$.senderId").value(SOME_SENDER_ID));
    }

    @Test
    @SneakyThrows
    void shouldListRemittancesBySenderId() {
        // given
        var pageable = PageRequest.of(0, 20);
        var domain = remittanceBuilder().build();
        var page = new PageImpl<>(List.of(domain), pageable, 1);

        var response = RemittanceResponse.builder()
                .id(SOME_REMITTANCE_DB_ID)
                .remittanceId(SOME_REMITTANCE_ID)
                .senderId(SOME_SENDER_ID)
                .status(RemittanceStatus.INITIATED)
                .build();

        given(listRemittancesQueryHandler.handle(SOME_SENDER_ID, pageable)).willReturn(page);
        given(remittanceApiMapper.toResponse(domain)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/remittances")
                        .param("senderId", SOME_SENDER_ID)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].remittanceId").value(SOME_REMITTANCE_ID.toString()))
                .andExpect(jsonPath("$.content[0].senderId").value(SOME_SENDER_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @SneakyThrows
    void shouldReturn404WhenRemittanceNotFound() {
        // given
        var unknownId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        given(getRemittanceQueryHandler.handle(unknownId))
                .willThrow(RemittanceNotFoundException.byId(unknownId));

        // when / then
        mockMvc.perform(get("/api/remittances/{remittanceId}", unknownId))
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
                .senderId(SOME_SENDER_ID)
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build();

        // when / then
        mockMvc.perform(post("/api/remittances")
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
                .senderId("")
                .recipientPhone("")
                .amountUsdc(null)
                .build();

        // when / then
        mockMvc.perform(post("/api/remittances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0003"));
    }
}
