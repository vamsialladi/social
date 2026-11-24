package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaServiceGroupPhotoTest {

    @TempDir
    Path uploadDirectory;

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

    private MediaService createService() {
        MediaService service = new MediaService();
        ReflectionTestUtils.setField(service, "uploadDir", uploadDirectory.toString());
        return service;
    }

    private MockMultipartFile imageFile(int size) {
        return new MockMultipartFile("file", "group.jpg", "image/jpeg", new byte[size]);
    }
}
