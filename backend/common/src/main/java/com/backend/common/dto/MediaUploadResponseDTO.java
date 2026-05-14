package com.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object returning the result of a successful media upload to
 * storage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload after successfully uploading a media file")
public class MediaUploadResponseDTO {

    @NotBlank(message = "File ID cannot be blank")
    @Schema(description = "Unique identifier assigned to the uploaded file", example = "img_992xkc1")
    private String fileId;

    @NotBlank(message = "File URL cannot be blank")
    @Schema(description = "Publicly accessible URL to view or download the file", example = "https://storage.buy-01.com/products/img_992xkc1.jpg")
    private String fileUrl;
}