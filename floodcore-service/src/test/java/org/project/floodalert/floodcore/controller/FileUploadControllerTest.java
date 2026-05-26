package org.project.floodalert.floodcore.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.floodalert.common.security.SecurityContextUtils;
import org.project.floodalert.floodcore.dto.response.FileUploadResponse;
import org.project.floodalert.floodcore.service.FirebaseStorageService;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @InjectMocks
    private FileUploadController fileUploadController;

    private UUID userId;
    private FileUploadResponse response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        response = mock(FileUploadResponse.class);
    }

    @Test
    void getUploadUrl_validExtension_callsService() {
        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(firebaseStorageService.generateUploadUrl(userId, "jpg"))
                    .thenReturn(response);

            fileUploadController.getUploadUrl("jpg");

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(firebaseStorageService).generateUploadUrl(userId, "jpg");
        }
    }

    @Test
    void getUploadUrl_serviceThrows_stillCallsService() {
        try (MockedStatic<SecurityContextUtils> mockedStatic =
                     mockStatic(SecurityContextUtils.class)) {

            mockedStatic.when(SecurityContextUtils::getCurrentUserIdAsUUID)
                    .thenReturn(userId);

            when(firebaseStorageService.generateUploadUrl(userId, "png"))
                    .thenThrow(new RuntimeException("Upload error"));

            try {
                fileUploadController.getUploadUrl("png");
            } catch (RuntimeException ignored) {
            }

            mockedStatic.verify(SecurityContextUtils::getCurrentUserIdAsUUID);
            verify(firebaseStorageService).generateUploadUrl(userId, "png");
        }
    }
}