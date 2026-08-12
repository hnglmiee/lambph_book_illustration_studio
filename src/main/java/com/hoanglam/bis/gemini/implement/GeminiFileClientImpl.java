package com.hoanglam.bis.gemini.implement;

import com.hoanglam.bis.enums.ErrorCode;
import com.hoanglam.bis.exceptions.ApiException;
import com.hoanglam.bis.gemini.dto.GeminiFile;
import com.hoanglam.bis.gemini.dto.GeminiFileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiFileClientImpl implements GeminiFileClient {

    private final RestClient geminiRestClient;

    @Override
    public GeminiFile uploadTextFile(byte[] content, String displayName) {
        String uploadUrl = startResumableUpload(content.length, displayName);
        return finalizeUpload(uploadUrl, content);
    }

    /**
     * Bước 1: gửi metadata, nhận về upload URL thật qua header "x-goog-upload-url".
     */
    private String startResumableUpload(int contentLength, String displayName) {
        ResponseEntity<Void> response = geminiRestClient.post()
                .uri("/upload/v1beta/files")
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", String.valueOf(contentLength))
                .header("X-Goog-Upload-Header-Content-Type", "text/plain")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("file", Map.of("display_name", displayName)))
                .retrieve()
                .toBodilessEntity();

        String uploadUrl = response.getHeaders().getFirst("x-goog-upload-url");
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new ApiException(ErrorCode.GEMINI_CALL_FAILED,
                    "Gemini did not return an upload URL", 502);
        }
        return uploadUrl;
    }

    /**
     * Bước 2: upload bytes thật lên URL nhận được, kèm command "upload, finalize".
     * uploadUrl là URL tuyệt đối trả về từ bước 1, KHÔNG dùng geminiRestClient (baseUrl khác),
     * nên tạo RestClient riêng cho lời gọi này.
     */
    private GeminiFile finalizeUpload(String uploadUrl, byte[] content) {
        GeminiFileUploadResponse result = RestClient.create()
                .post()
                .uri(uploadUrl)
                .header("Content-Length", String.valueOf(content.length))
                .header("X-Goog-Upload-Offset", "0")
                .header("X-Goog-Upload-Command", "upload, finalize")
                .body(content)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    throw new ApiException(ErrorCode.GEMINI_CALL_FAILED,
                            "Failed to finalize file upload to Gemini", 502);
                })
                .body(GeminiFileUploadResponse.class);

        if (result == null || result.getFile() == null) {
            throw new ApiException(ErrorCode.GEMINI_CALL_FAILED,
                    "Gemini returned empty file upload response", 502);
        }
        return result.getFile();
    }
}