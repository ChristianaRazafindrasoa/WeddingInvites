package com.wedding.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.wedding.data.PhotoRepository;
import com.wedding.data.WeddingInfoRepository;
import com.wedding.domain.RSVPService;
import com.wedding.dto.RSVPRequest;
import com.wedding.dto.RSVPResponse;
import com.wedding.model.Photo;
import com.wedding.model.WeddingInfo;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@RestController
@RequestMapping("api")
@CrossOrigin(origins = "http://localhost:3000")
public class WeddingController {
    private WeddingInfoRepository infoRepo;
    private PhotoRepository photoRepo;
    private final RSVPService rsvpService;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public WeddingController(
            WeddingInfoRepository infoRepo, 
            PhotoRepository photoRepo,
            RSVPService rsvpService,
            @Value("${stripe.secret.key}") String stripeApiKey,
            @Value("${aws.region}") String region,
            @Value("${aws.bucket}") String bucketName) {
        this.infoRepo = infoRepo;
        this.photoRepo = photoRepo;
        this.rsvpService = rsvpService;
        Stripe.apiKey = stripeApiKey;
        this.s3Presigner = S3Presigner.builder()
            .region(Region.of(region))
            .build();
        this.bucketName = bucketName;
    }

    @GetMapping("/info")
    public WeddingInfo getWeddingInfo() {
        return infoRepo.findAll().stream().findFirst().orElseThrow();
    }

    @GetMapping("/rsvp")
    public ResponseEntity<RSVPResponse> getByToken(@RequestParam String token) {
        try {
            return ResponseEntity.ok(rsvpService.findByToken(token));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/rsvp")
    public ResponseEntity<RSVPResponse> submit(@RequestBody RSVPRequest request) {
        try {
            RSVPResponse response = rsvpService.submit(request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                new RSVPResponse(
                        null,
                        null,
                        false,
                        e.getMessage()
                )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new RSVPResponse(
                        null,
                        null,
                        false,
                        e.getMessage()
                )
            );
        }
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

    @GetMapping("/photo-gallery")
    public List<String> getApprovedPicturesURL() {
        List<String> urls = new ArrayList<>();
        for (Photo photo : photoRepo.findByIsApproved(true)) {
            urls.add(getPresignedURL(photo.getS3Key()));
        }
        return urls;
    }  

    private String getPresignedURL(String s3key){
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(s3key)
            .build();
        PresignedGetObjectRequest presignedRequest =
            s3Presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest));
        return presignedRequest.url().toString();      
    }
}