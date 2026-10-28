package com.ruben.shop.order;

import com.ruben.shop.order.application.OrderService;
import com.ruben.shop.order.application.PlaceOrderCommand;
import com.ruben.shop.order.domain.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerSecurityTest {

    private static final String BODY = """
            {"productId":"LAPTOP","quantity":1,"amount":900}""";

    @Autowired
    MockMvc mvc;
    @Autowired
    OrderService service;

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsTakenFromTheToken() throws Exception {
        mvc.perform(post("/orders").with(jwt().jwt(j -> j.claim("preferred_username", "user123")))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.customerId").value("user123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void usersOnlySeeTheirOwnOrders() throws Exception {
        var other = service.placeOrder(new PlaceOrderCommand("someone-else", "PHONE", 1, Money.of("10")));

        mvc.perform(get("/orders/" + other.id().value()).with(jwt().jwt(j -> j.claim("preferred_username", "user123"))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/orders").with(jwt().jwt(j -> j.claim("preferred_username", "user123"))))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
