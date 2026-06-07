package com.wedding.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestBody Map<String, Object> payload) {

        Long amount = ((Long) payload.get("amount")).longValue();
        String currency = (String) payload.get("currency");
        String productName = (String) payload.get("productName");
        String successUrl = (String) payload.get("successUrl");
        String cancelUrl = (String) payload.get("cancelUrl");

        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(currency)
                            .setUnitAmount(amount)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(productName)
                                    .build())
                            .build())
                    .build())
            .build();

        try {
            Session session = Session.create(params);
            return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "checkoutUrl", session.getUrl()));
        } catch (StripeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

