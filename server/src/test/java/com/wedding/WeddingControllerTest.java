// package com.wedding;

// import com.stripe.exception.ApiConnectionException;
// import com.stripe.model.checkout.Session;
// import com.stripe.param.checkout.SessionCreateParams;
// import com.wedding.controller.WeddingController;
// import com.wedding.data.PhotoRepository;
// import com.wedding.data.WeddingInfoRepository;
// import com.wedding.domain.RSVPService;
// import com.wedding.model.Photo;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.ArgumentCaptor;
// import org.mockito.MockedStatic;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.test.util.ReflectionTestUtils;
// import software.amazon.awssdk.services.s3.presigner.S3Presigner;
// import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
// import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
// import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

// import java.net.URI;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Map;
// import java.util.function.Consumer;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.mockStatic;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// class WeddingControllerTest {
//     private PhotoRepository photoRepo;
//     private S3Presigner s3Presigner;
//     private WeddingController controller;

//     @BeforeEach
//     void setUp() {
//         WeddingInfoRepository infoRepo = mock(WeddingInfoRepository.class);
//         photoRepo = mock(PhotoRepository.class);
//         RSVPService rsvpService = mock(RSVPService.class);

//         controller = new WeddingController(
//                 infoRepo, photoRepo, rsvpService,
//                 "sk_test_dummy", "us-east-1", "test-bucket");

//         s3Presigner = mock(S3Presigner.class);
//         ReflectionTestUtils.setField(controller, "s3Presigner", s3Presigner);
//     }

//     @Test
//     void createCheckoutSessionReturnsUrlWhenPayloadValid() throws Exception {
//         Session session = mock(Session.class);
//         when(session.getUrl()).thenReturn("https://checkout.stripe.com/session123");

//         try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
//             mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

//             Map<String, Object> payload = Map.of(
//                     "amount", "50",
//                     "token", "abc123",
//                     "name", "Foo Test");

//             ResponseEntity<Map<String, String>> response = controller.createCheckoutSession(payload);
//             assertEquals(HttpStatus.OK, response.getStatusCode());
//             assertEquals("https://checkout.stripe.com/session123", response.getBody().get("url"));
//             mocked.verify(() -> Session.create(any(SessionCreateParams.class)));
//         }
//     }

//     @Test
//     void createCheckoutSessionThrowsWhenAmountNotPositive() {
//         Map<String, Object> payload = Map.of(
//                 "amount", "0",
//                 "token", "abc123",
//                 "name", "Foo Test");
//         IllegalStateException ex = assertThrows(
//                 IllegalStateException.class,
//                 () -> controller.createCheckoutSession(payload));
//         assertEquals("Minimum donation amount is $1", ex.getMessage());
//     }

//     @Test
//     void getSessionReturnsAmountWhenSessionExists() throws Exception {
//         Session session = mock(Session.class);
//         when(session.getAmountTotal()).thenReturn(5000L);
//         try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
//             mocked.when(() -> Session.retrieve("sess_123")).thenReturn(session);
//             ResponseEntity<Map<String, Object>> response = controller.getSession("sess_123");
//             assertEquals(HttpStatus.OK, response.getStatusCode());
//             assertEquals(50.0, response.getBody().get("amount"));
//         }
//     }

//     @Test
//     void getSessionReturnsNotFoundWhenStripeFails() throws Exception {
//         try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
//             mocked.when(() -> Session.retrieve("bad")).thenThrow(new ApiConnectionException("boom"));
//             ResponseEntity<Map<String, Object>> response = controller.getSession("bad");
//             assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//             assertEquals("Checkout session not found", response.getBody().get("error"));
//         }
//     }

//     @Test
//     void putPresignedURLReturnsUploadUrlAndKey() throws Exception {
//         PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
//         when(presigned.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/upload").toURL());
//         when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

//         Map<String, String> body = Map.of(
//                 "contentType", "image/png",
//                 "fileName", "cake.png");

//         ResponseEntity<Map<String, String>> response = controller.putPresignedURL(body);
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertEquals("https://s3.amazonaws.com/test-bucket/upload", response.getBody().get("uploadUrl"));
//         assertTrue(response.getBody().get("s3Key").endsWith("cake.png"));
//         verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
//     }

//     @Test
//     void savePhotoPersistsUnapprovedPhotoAndReturnsMessage() {
//         Map<String, String> body = Map.of("s3Key", "uuid-cake.png");
//         ResponseEntity<?> response = controller.savePhoto(body);
//         assertEquals(HttpStatus.OK, response.getStatusCode());
//         assertEquals("Photo saved successfully", ((Map<?, ?>) response.getBody()).get("message"));

//         ArgumentCaptor<Photo> captor = ArgumentCaptor.forClass(Photo.class);
//         verify(photoRepo).save(captor.capture());
//         Photo saved = captor.getValue();
//         assertEquals("uuid-cake.png", saved.getS3Key());
//         assertFalse(saved.isApproved(), "newly saved photos should await approval");
//         assertNotNull(saved.getUploadedAt());
//     }

//     @Test
//     @SuppressWarnings("unchecked")
//     void getApprovedPicturesURLReturnsPresignedUrls() throws Exception {
//         Photo photo = new Photo("key1.png", LocalDateTime.now(), true);
//         when(photoRepo.findByIsApproved(true)).thenReturn(List.of(photo));

//         PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
//         when(presigned.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/key1.png").toURL());
//         when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presigned);

//         List<String> urls = controller.getApprovedPicturesURL();
//         assertEquals(1, urls.size());
//         assertEquals("https://s3.amazonaws.com/test-bucket/key1.png", urls.get(0));
//         verify(photoRepo).findByIsApproved(true);
//     }
// }