package org.project.floodalert.floodcore.service;

import org.project.floodalert.floodcore.dto.response.FileUploadResponse;

import java.util.UUID;

public interface FirebaseStorageService {
    
    /**
     * @param userId ID của người dùng
     * @param extension Định dạng file (jpg, png, jpeg, webp)
     * @return FileUploadResponse chứa uploadUrl và publicUrl
     */
    FileUploadResponse generateUploadUrl(UUID userId, String extension);
}
