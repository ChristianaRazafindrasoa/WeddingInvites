package com.wedding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.RateLimitException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@SpringBootTest
@AutoConfigureMockMvc
class StripeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_PAYLOAD =
            "{\"amount\": \"50\", \"token\": \"abc123\", \"name\": \"Foo Test\"}";

    @Test
    void createCheckoutSessionReturnsUrlWhenPayloadValid() throws Exception {
        Session session = mock(Session.class);
        when(session.getUrl()).thenReturn("https://checkout.stripe.com/session123");
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);
            mockMvc.perform(post("/api/honeymoon-fund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PAYLOAD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/session123"));
        }
    }

    @Test
    void createCheckoutSessionReturnsBadRequestWhenAmountNotPositive() throws Exception {
        mockMvc.perform(post("/api/honeymoon-fund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": \"0\", \"token\": \"abc123\", \"name\": \"Foo Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Minimum donation amount is $1"));
    }

    @Test
    void createCheckoutSessionReturnsBadRequestWhenAmountMissing() throws Exception {
        mockMvc.perform(post("/api/honeymoon-fund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"abc123\", \"name\": \"Foo Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount is required."));
    }

    @Test
    void createCheckoutSessionReturnsBadRequestWhenAmountNotNumeric() throws Exception {
        mockMvc.perform(post("/api/honeymoon-fund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": \"abc\", \"token\": \"abc123\", \"name\": \"Foo Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount must be a valid number."));
    }

    @Test
    void getSessionReturnsAmountWhenSessionExists() throws Exception {
        Session session = mock(Session.class);
        when(session.getAmountTotal()).thenReturn(5000L);
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.retrieve("sess_123")).thenReturn(session);
            mockMvc.perform(get("/api/checkout-session/sess_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(50));
        }
    }

    @Test
    void getSessionReturnsSameAmountOnDuplicateRequest() throws Exception {
        Session session = mock(Session.class);
        when(session.getAmountTotal()).thenReturn(5000L);
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.retrieve("sess_123")).thenReturn(session);
            mockMvc.perform(get("/api/checkout-session/sess_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(50));
            mockMvc.perform(get("/api/checkout-session/sess_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(50));
        }
    }

    @Test
    void returns402WhenCardDeclined() throws Exception {
        CardException ex = mock(CardException.class);
        when(ex.getMessage()).thenReturn("Your card was declined.");
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(ex);
            mockMvc.perform(post("/api/honeymoon-fund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PAYLOAD))
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.error").value("Your card was declined."));
        }
    }

    @Test
    void returns400WhenInvalidRequest() throws Exception {
        InvalidRequestException ex = mock(InvalidRequestException.class);
        when(ex.getMessage()).thenReturn("Invalid currency.");
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(ex);
            mockMvc.perform(post("/api/honeymoon-fund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PAYLOAD))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid currency."));
        }
    }

    @Test
    void returns500WhenAuthenticationFails() throws Exception {
        AuthenticationException ex = mock(AuthenticationException.class);
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(ex);
            mockMvc.perform(post("/api/honeymoon-fund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PAYLOAD))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Payment service configuration error."));
        }
    }

    @Test
    void returns429WhenRateLimited() throws Exception {
        RateLimitException ex = mock(RateLimitException.class);
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(ex);
            mockMvc.perform(post("/api/honeymoon-fund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PAYLOAD))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error").value("Too many requests. Please try again later."));
        }
    }

    @Test
    void returns502WhenStripeUnreachable() throws Exception {
        ApiConnectionException ex = mock(ApiConnectionException.class);
        when(ex.getMessage()).thenReturn("Unable to connect to Stripe.");
        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(ex);
            mockMvc.perform(post("/api/honeymoon-fund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PAYLOAD))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error").value("Unable to connect to Stripe."));
        }
    }
}
