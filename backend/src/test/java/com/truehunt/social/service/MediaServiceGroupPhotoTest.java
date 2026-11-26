package com.truehunt.social.service;

import com.truehunt.social.dto.ContentSafetyResult;
import com.truehunt.social.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaServiceGroupPhotoTest {

    @TempDir
    Path uploadDirectory;

    private ContentSafetyService contentSafetyService;

    @Test
    void acceptsGroupPhotosUpToTenMegabytes() {
        MediaService service = createService();
        MockMultipartFile file = imageFile(10 * 1024 * 1024);

        assertDoesNotThrow(() -> service.uploadFile(file, "connection-group-photo"));
    }

    @Test
    void acceptsBmpGroupPhotos() {
        MediaService service = createService();
        MockMultipartFile file = new MockMultipartFile("file", "group.bmp", "image/bmp", new byte[]{1, 2, 3});

        assertDoesNotThrow(() -> service.uploadFile(file, "connection-group-photo"));
    }

    @Test
    void rejectsGroupPhotosLargerThanTenMegabytes() {
        MediaService service = createService();
        MockMultipartFile file = imageFile(10 * 1024 * 1024 + 1);

        assertThrows(BadRequestException.class, () -> service.uploadFile(file, "connection-group-photo"));
    }

    @Test
    void keepsTheExistingFiveMegabyteProfileImageLimit() {
        MediaService service = createService();
        MockMultipartFile file = imageFile(5 * 1024 * 1024 + 1);

        assertThrows(BadRequestException.class, () -> service.uploadFile(file, "profile-image"));
    }

    @Test
    void keepsBmpUnsupportedForProfileImages() {
        MediaService service = createService();
        MockMultipartFile file = new MockMultipartFile("file", "profile.bmp", "image/bmp", new byte[]{1, 2, 3});

        assertThrows(BadRequestException.class, () -> service.uploadFile(file, "profile-image"));
    }

    @Test
    void rejectsAdultImagesBeforeSavingThem() throws IOException {
        MediaService service = createService();
        when(contentSafetyService.checkImage(any(byte[].class)))
                .thenReturn(ContentSafetyResult.rejected("Adult content is not allowed."));

        assertThrows(
                BadRequestException.class,
                () -> service.uploadFile(imageFile(3), "article-image")
        );

        assertTrue(Files.notExists(uploadDirectory) || isDirectoryEmpty(uploadDirectory));
    }

    private MediaService createService() {
        contentSafetyService = mock(ContentSafetyService.class);
        when(contentSafetyService.checkImage(any(byte[].class)))
                .thenReturn(ContentSafetyResult.safe());
        MediaService service = new MediaService(contentSafetyService);
        ReflectionTestUtils.setField(service, "uploadDir", uploadDirectory.toString());
        return service;
    }

    private MockMultipartFile imageFile(int size) {
        return new MockMultipartFile("file", "group.jpg", "image/jpeg", new byte[size]);
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.findAny().isEmpty();
        }
    }
}
