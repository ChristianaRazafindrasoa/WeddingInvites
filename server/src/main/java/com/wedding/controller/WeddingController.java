package com.wedding.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.wedding.data.WeddingInfoRepository;
import com.wedding.model.WeddingInfo;

@RestController
@RequestMapping("api")
@CrossOrigin(origins = "http://localhost:3000")
public class WeddingController {
    private WeddingInfoRepository infoRepo;

    public WeddingController(
            WeddingInfoRepository infoRepo, 
            @Value("${stripe.secret.key}") String stripeApiKey) {
        this.infoRepo = infoRepo;
        Stripe.apiKey = stripeApiKey;
    }

    @GetMapping("/info")
    public WeddingInfo getWeddingInfo() {
        return infoRepo.findAll().stream().findFirst().orElseThrow();
    }

    @PostMapping("/honeymoon-fund")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @RequestBody Map<String, Object> payload) throws StripeException{
        Long amountValue = Long.valueOf(payload.get("amount").toString());
        if (amountValue <= 0) {
            throw new IllegalStateException("Minimum donation amount is $1");
        }

        String successUrl = "http://localhost:3000/honeymoon-fund?token=" +
            payload.get("token") + "&success=true&id={CHECKOUT_SESSION_ID}";
        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrl)
            .putMetadata("name", payload.get("name").toString())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(amountValue * 100)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Honeymoon Fund Contribution")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build();

        Session session = Session.create(params);
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    @GetMapping("/checkout-session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(
        @PathVariable String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return ResponseEntity.ok(Map.of(
                "amount", session.getAmountTotal() / 100.0
            ));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Checkout session not found" 
            ));
        }
    }
}

