package com.social.service;

import com.social.dto.response.MediaUploadResponse;
import com.social.dto.response.ScopedUploadUrlResponse;
import com.social.entity.MediaFile;
import com.social.exception.BadRequestException;
import com.social.repository.MediaFileRepository;
import com.social.service.impl.MediaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaFileRepository mediaFileRepository;

    private MediaServiceImpl mediaService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mediaService = new MediaServiceImpl(mediaFileRepository);
        ReflectionTestUtils.setField(mediaService, "storageDir", tempDir.toString());
        ReflectionTestUtils.setField(mediaService, "baseUrl", "/api/media/files/");
        ReflectionTestUtils.setField(mediaService, "allowedContentTypes", List.of(
                "image/png", "image/jpeg", "video/mp4", "application/pdf"
        ));
        mediaService.init();
    }

    @Test
    void uploadFile_validPngImage_succeeds() {
        // Simple PNG header bytes: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", pngBytes
        );

        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaUploadResponse response = mediaService.uploadFile(file, "user-100");

        assertNotNull(response);
        assertEquals("test.png", response.getFileName());
        assertEquals("image/png", response.getContentType());
        assertNotNull(response.getObjectKey());
        assertTrue(response.getUrl().startsWith("/api/media/files/"));
    }

    @Test
    void uploadFile_emptyFile_throwsBadRequestException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThrows(BadRequestException.class, () -> 
                mediaService.uploadFile(emptyFile, "user-100"));
    }

    @Test
    void loadFileAsResource_invalidPathTraversal_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> 
                mediaService.loadFileAsResource("../../../etc/passwd"));
    }

    @Test
    void generateScopedUploadUrl_valid_succeeds() {
        ScopedUploadUrlResponse response = mediaService.generateScopedUploadUrl(
                "photo.png", "image/png", 1024, "user-100"
        );

        assertNotNull(response);
        assertNotNull(response.getUploadUrl());
        assertNotNull(response.getObjectKey());
        assertNotNull(response.getExpiresAt());
    }
}
