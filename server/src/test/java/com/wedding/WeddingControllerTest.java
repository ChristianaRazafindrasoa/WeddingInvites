package com.wedding;

import com.wedding.controller.WeddingController;
import com.wedding.data.NoteRepository;
import com.wedding.data.PhotoRepository;
import com.wedding.data.WeddingInfoRepository;
import com.wedding.domain.WeddingService;
import com.wedding.dto.RSVPResponse;
import com.wedding.model.Photo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeddingControllerTest {
    private PhotoRepository photoRepo;
    private WeddingService weddingService;
    private S3Presigner s3Presigner;
    private WeddingController controller;

    @BeforeEach
    void setUp() {
        WeddingInfoRepository infoRepo = mock(WeddingInfoRepository.class);
        photoRepo = mock(PhotoRepository.class);
        NoteRepository noteRepo = mock(NoteRepository.class);
        weddingService = mock(WeddingService.class);
        controller = new WeddingController(
                infoRepo, photoRepo, noteRepo, weddingService,
                "sk_test_dummy", "us-east-1", "test-bucket", "http://localhost:3000", "test");
        s3Presigner = mock(S3Presigner.class);
        ReflectionTestUtils.setField(controller, "s3Presigner", s3Presigner);
    }

    @Test
    void putPresignedURLReturnsUploadUrlAndKey() throws Exception {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/upload").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);
        when(weddingService.findByToken("abc123"))
                .thenReturn(new RSVPResponse("Foo Test", null, false, false, Optional.empty()));
        Map<String, String> body = Map.of(
                "contentType", "image/png",
                "fileName", "cake.png",
                "token", "abc123");
        ResponseEntity<Map<String, String>> response = controller.putPresignedURL(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("https://s3.amazonaws.com/test-bucket/upload", response.getBody().get("uploadUrl"));
        assertTrue(response.getBody().get("s3Key").endsWith("cake.png"));
        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void savePhotoPersistsUnapprovedPhotoAndReturnsMessage() {
        when(weddingService.findByToken("abc123"))
                .thenReturn(new RSVPResponse("Foo Test", null, false, false, Optional.empty()));
        Map<String, String> body = Map.of("s3Key", "uuid-cake.png", "token", "abc123");
        ResponseEntity<?> response = controller.savePhoto(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Photo saved successfully", ((Map<?, ?>) response.getBody()).get("message"));
        ArgumentCaptor<Photo> captor = ArgumentCaptor.forClass(Photo.class);
        verify(photoRepo).save(captor.capture());
        Photo saved = captor.getValue();
        assertEquals("uuid-cake.png", saved.getS3Key());
        assertTrue(saved.isApproved(), "photos should be auto-approved on save");
        assertNotNull(saved.getUploadedAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getApprovedPicturesURLReturnsPresignedUrls() throws Exception {
        Photo photo = new Photo("key1.png", LocalDateTime.now(), "Jane & Bob", true);
        when(photoRepo.findByIsApproved(true)).thenReturn(List.of(photo));
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/key1.png").toURL());
        when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presigned);
        List<Map<String, String>> result = controller.getApprovedPicturesURL();
        assertEquals(1, result.size());
        assertEquals("https://s3.amazonaws.com/test-bucket/key1.png", result.get(0).get("url"));
        assertEquals("Jane & Bob", result.get(0).get("uploadedBy"));
        verify(photoRepo).findByIsApproved(true);
    }
}
