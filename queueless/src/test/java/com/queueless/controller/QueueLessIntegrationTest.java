
package com.queueless.controller;

import com.queueless.dto.QueueTokenResponse;
import com.queueless.dto.ServiceResponse;
import com.queueless.entity.TokenStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QueueLessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createService(String name) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", name)
                        )))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("id").asLong();
    }

    private QueueTokenResponse createToken(
            String name, Long serviceId) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "customerName", name,
                                        "serviceId", serviceId
                                )
                        )))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                QueueTokenResponse.class
        );
    }

    @Test
    void createServiceSuccessfully() throws Exception {

        String name = "Integration Service " + System.nanoTime();

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", name)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getAllServicesSuccessfully() throws Exception {

        createService("Integration List " + System.nanoTime());

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)
                ));
    }

    @Test
    void getServiceByIdSuccessfully() throws Exception {

        Long serviceId = createService(
                "Integration Lookup " + System.nanoTime()
        );

        mockMvc.perform(get("/api/services/" + serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId));
    }

    @Test
    void deactivateAndReactivateService() throws Exception {

        Long serviceId = createService(
                "Integration Status " + System.nanoTime()
        );

        mockMvc.perform(patch(
                        "/api/services/" + serviceId + "/status"
                ).param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch(
                        "/api/services/" + serviceId + "/status"
                ).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void generateTokenSuccessfully() throws Exception {

        Long serviceId = createService(
                "Integration Token " + System.nanoTime()
        );

        QueueTokenResponse token = createToken("Ahmed", serviceId);

        assertEquals("Ahmed", token.customerName());
        assertEquals(TokenStatus.WAITING, token.status());
        assertEquals(serviceId, token.serviceId());
    }

    @Test
    void rejectTokenWithBlankCustomerName() throws Exception {

        Long serviceId = createService(
                "Integration Validation " + System.nanoTime()
        );

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "customerName", "",
                                        "serviceId", serviceId
                                )
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectTokenWithInvalidServiceId() throws Exception {

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "customerName", "Ahmed",
                                        "serviceId", -1
                                )
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retrieveWaitingTokensSuccessfully() throws Exception {

        Long serviceId = createService(
                "Integration Waiting " + System.nanoTime()
        );

        createToken("Ahmed", serviceId);
        createToken("Bilal", serviceId);

        mockMvc.perform(get("/api/tokens/waiting/" + serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    void retrieveQueuePositionSuccessfully() throws Exception {

        Long serviceId = createService(
                "Integration Position " + System.nanoTime()
        );

        createToken("Ahmed", serviceId);
        QueueTokenResponse secondToken = createToken("Bilal", serviceId);

        mockMvc.perform(get(
                        "/api/tokens/" + secondToken.id() + "/position"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peopleAhead").value(1));
    }

    @Test
    void completeAndSkipTokensSuccessfully() throws Exception {

        Long serviceId = createService(
                "Integration Lifecycle " + System.nanoTime()
        );

        QueueTokenResponse first = createToken("Ahmed", serviceId);
        QueueTokenResponse second = createToken("Bilal", serviceId);

        // Call Ahmed
        mockMvc.perform(post("/api/queues/" + serviceId + "/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SERVING"));

        // Complete Ahmed
        mockMvc.perform(put(
                        "/api/queues/tokens/" + first.id() + "/complete"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Call Bilal
        mockMvc.perform(post("/api/queues/" + serviceId + "/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second.id()));

        // Skip Bilal
        mockMvc.perform(put(
                        "/api/queues/tokens/" + second.id() + "/skip"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));
    }
}
