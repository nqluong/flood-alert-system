package org.project.floodalert.floodcore.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @param uploadUrl URL để frontend PUT file lên (có thời hạn 15 phút)
 * @param fileUrl URL public để xem file sau khi upload thành công
 * @param filePath Đường dẫn file trong storage (để lưu vào database)
 * @param expiresInMinutes Thời gian hết hạn của uploadUrl (phút)
 */
public record FileUploadResponse(
        @JsonProperty("upload_url")
        String uploadUrl,
        
        @JsonProperty("file_url")
        String fileUrl,
        
        @JsonProperty("file_path")
        String filePath,
        
        @JsonProperty("expires_in_minutes")
        int expiresInMinutes
) {
}
