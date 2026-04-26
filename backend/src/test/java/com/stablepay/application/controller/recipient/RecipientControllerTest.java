package com.stablepay.application.controller.recipient;

import static com.stablepay.testutil.RemittanceFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_NAME;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.SecurityTestBase.asUser;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.stablepay.application.controller.recipient.mapper.RecipientApiMapper;
import com.stablepay.application.controller.recipient.mapper.RecipientApiMapperImpl;
import com.stablepay.domain.remittance.handler.GetRecentRecipientsHandler;
import com.stablepay.domain.remittance.model.RecentRecipient;
import com.stablepay.testutil.TestClockConfig;
import com.stablepay.testutil.TestSecurityConfig;

import lombok.SneakyThrows;

@WebMvcTest(RecipientController.class)
@Import({TestClockConfig.class, TestSecurityConfig.class, RecipientApiMapperImpl.class})
class RecipientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetRecentRecipientsHandler getRecentRecipientsHandler;

    @MockitoSpyBean
    private RecipientApiMapper recipientApiMapper;

    @Test
    @SneakyThrows
    void shouldReturnRecentRecipients() {
        // given
        var recipient = RecentRecipient.builder()
                .name(SOME_RECIPIENT_NAME)
                .phone(SOME_RECIPIENT_PHONE)
                .lastSentAt(SOME_CREATED_AT)
                .build();
        given(getRecentRecipientsHandler.handle(SOME_SENDER_ID, 10))
                .willReturn(List.of(recipient));

        // when / then
        mockMvc.perform(get("/api/recipients/recent")
                        .with(asUser(SOME_SENDER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(SOME_RECIPIENT_NAME))
                .andExpect(jsonPath("$[0].phone").value(SOME_RECIPIENT_PHONE))
                .andExpect(jsonPath("$[0].lastSentAt").exists());
    }

    @Test
    @SneakyThrows
    void shouldReturnEmptyArrayWhenNoRecipients() {
        // given
        given(getRecentRecipientsHandler.handle(SOME_SENDER_ID, 10))
                .willReturn(List.of());

        // when / then
        mockMvc.perform(get("/api/recipients/recent")
                        .with(asUser(SOME_SENDER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @SneakyThrows
    void shouldRespectCustomLimitParameter() {
        // given
        given(getRecentRecipientsHandler.handle(SOME_SENDER_ID, 5))
                .willReturn(List.of());

        // when / then
        mockMvc.perform(get("/api/recipients/recent")
                        .with(asUser(SOME_SENDER_ID))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @SneakyThrows
    void shouldReturn401WhenNoBearer() {
        // when / then
        mockMvc.perform(get("/api/recipients/recent"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SP-0040"));
    }
}
