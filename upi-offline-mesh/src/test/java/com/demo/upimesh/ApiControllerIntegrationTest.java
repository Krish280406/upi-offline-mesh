package com.demo.upimesh;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void demoSendRejectsNegativeAmount() throws Exception {
        Map<String, Object> body = Map.of(
                "senderVpa", "alice@demo", "receiverVpa", "bob@demo",
                "amount", -50, "pin", "1234");
        mockMvc.perform(post("/api/demo/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void demoSendAcceptsValidPayment() throws Exception {
        Map<String, Object> body = Map.of(
                "senderVpa", "alice@demo", "receiverVpa", "bob@demo",
                "amount", 100, "pin", "1234");
        mockMvc.perform(post("/api/demo/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packetId").exists());
    }

    @Test
    void bridgeIngestRejectsMissingSignature() throws Exception {
        String rawBody = "{\"packetId\":\"test\",\"ttl\":1,\"createdAt\":1,\"ciphertext\":\"abc\"}";
        mockMvc.perform(post("/api/bridge/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawBody))
                .andExpect(status().isUnauthorized());
    }
}