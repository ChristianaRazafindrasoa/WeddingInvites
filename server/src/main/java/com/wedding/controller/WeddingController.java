package com.wedding.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import com.wedding.data.NoteRepository;
import com.wedding.data.PhotoRepository;
import com.wedding.data.WeddingInfoRepository;
import com.wedding.domain.WeddingService;
import com.wedding.dto.RSVPRequest;
import com.wedding.dto.RSVPResponse;
import com.wedding.exception.WeddingException;
import com.wedding.model.Note;
import com.wedding.model.Photo;
import com.wedding.model.WeddingInfo;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@RestController
@RequestMapping("api")
@CrossOrigin(origins = "${app.cors.origins}")
public class WeddingController {
    private WeddingInfoRepository infoRepo;
    private PhotoRepository photoRepo;
    private NoteRepository noteRepo;
    private final WeddingService weddingService;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String baseUrl;

    public WeddingController(
            WeddingInfoRepository infoRepo,
            PhotoRepository photoRepo,
            NoteRepository noteRepo,
            WeddingService weddingService,
            @Value("${stripe.secret.key}") String stripeApiKey,
            @Value("${aws.region}") String region,
            @Value("${aws.bucket}") String bucketName,
            @Value("${app.base-url}") String baseUrl) {
        this.infoRepo = infoRepo;
        this.photoRepo = photoRepo;
        this.noteRepo = noteRepo;
        this.weddingService = weddingService;
        Stripe.apiKey = stripeApiKey;
        this.s3Presigner = S3Presigner.builder()
            .region(Region.of(region))
            .build();
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/info")
    public WeddingInfo getWeddingInfo() {
        return infoRepo.findAll().stream().findFirst().orElseThrow();
    }

    @GetMapping("/rsvp")
    public ResponseEntity<RSVPResponse> getByToken(@RequestParam String token) {
        RSVPResponse response = weddingService.findByToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rsvp")
    public ResponseEntity<RSVPResponse> submit(@RequestBody RSVPRequest request) {
        RSVPResponse response = weddingService.submit(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/honeymoon-fund")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @RequestBody Map<String, Object> payload) throws StripeException{
        long amountInCents = weddingService.validateCheckout((String) payload.get("amount"));
        String token = (String) payload.get("token");
        check(token != null && !token.isBlank(), "Token is required");
        weddingService.findByToken(token);
        String successUrl = token != null
            ? baseUrl + "/?token=" + token + "&success=true&id={CHECKOUT_SESSION_ID}"
            : baseUrl + "/?success=true&id={CHECKOUT_SESSION_ID}";
        String cancelUrl = token != null
            ? baseUrl + "/?token=" + token
            : baseUrl + "/";
            
        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(amountInCents * 100)
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
    public ResponseEntity<Map<String, Object>> getSession (
            @PathVariable String sessionId) throws StripeException {
        Session session = Session.retrieve(sessionId);
        long amount = session.getAmountTotal();
        return ResponseEntity.ok(Map.of("amount", amount / 100L));
    }

    @GetMapping("/photo-gallery")
    public List<Map<String, String>> getApprovedPicturesURL() {
        List<Map<String, String>> response = new ArrayList<>();
        for (Photo photo : photoRepo.findByIsApproved(true)) {
            Map<String, String> item = new HashMap<>();
            item.put("url", getPresignedURL(photo.getS3Key()));
            item.put("uploadedBy", photo.getUploadedBy());
            response.add(item);
        }
        return response;
    }  

    private String getPresignedURL(String s3key) {
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

    @PostMapping("/photos/upload")
    public ResponseEntity<Map<String, String>> putPresignedURL(
            @RequestBody Map<String, String> body) {
        String token = body.get("token");
        check(token != null && !token.isBlank(), "Token is required");
        weddingService.findByToken(token);
        String contentType = body.get("contentType");
        String fileName = body.get("fileName");
        String key = "test/" + UUID.randomUUID() + "-" + fileName;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build();
        PutObjectPresignRequest presignRequest =
            PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest =
            s3Presigner.presignPutObject(presignRequest);
        return ResponseEntity.ok(Map.of(
            "uploadUrl", presignedRequest.url().toString(),
            "s3Key", key
        ));
    }

    @PostMapping("/photos/save")
    public ResponseEntity<Map<String, String>> savePhoto(
            @RequestBody Map<String, String> body) {
        String s3Key = body.get("s3Key");
        String token = body.get("token");
        check(s3Key != null, "Key cannot be null");
        check(token != null, "Token cannot be null");
        String uploadedBy = null;
        RSVPResponse response = weddingService.findByToken(token);
        String plusOne = response.plusOneName();
        uploadedBy = response.mainGuestName();
        if (plusOne != null) {
            uploadedBy += " & " + plusOne;
        }
        Photo photo = new Photo(s3Key, LocalDateTime.now(), uploadedBy, true);
        photoRepo.save(photo);
        return ResponseEntity.ok(Map.of("message", "Photo saved successfully"));
    }

    @GetMapping("/guestbook")
    public List<Note> getNoteEntries() {
        return noteRepo.findAll();
    }

    @PostMapping("/guestbook")
    public ResponseEntity<Note> postNoteEntry(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String message = body.get("message");
        check(token != null && !token.isBlank(), "Token cannot be null");
        check(message != null && !message.isBlank(), "Message cannot be null");
        weddingService.findByToken(token);
        String providedName = body.get("name");
        String name = (providedName != null && !providedName.isBlank()) ? providedName : "Anonymous";
        Note note = noteRepo.save(new Note(name, message));
        return ResponseEntity.status(HttpStatus.OK).body(note);
    }

    private void check(boolean expression, String message) {
        if (!expression) {
            throw new WeddingException(HttpStatus.BAD_REQUEST, message);
        }
    }
}