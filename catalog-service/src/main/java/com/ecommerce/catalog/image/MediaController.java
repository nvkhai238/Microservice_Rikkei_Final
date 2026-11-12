package com.ecommerce.catalog.image;

import com.ecommerce.catalog.product.ProductDtos;
import com.ecommerce.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MediaController {

    private final ImageService imageService;

    /** Phục vụ file ảnh. nosniff: trình duyệt không được "đoán" kiểu file khác (chống XSS qua file upload). */
    @GetMapping("/api/v1/media/{fileName}")
    public ResponseEntity<Resource> serve(@PathVariable String fileName) {
        ImageService.StoredImage image = imageService.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'")
                .body(new FileSystemResource(image.path()));
    }

    @PostMapping(value = "/api/v1/admin/catalog/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDtos.ImageResponse> upload(@PathVariable UUID productId,
                                                         @RequestPart("file") MultipartFile file,
                                                         @RequestParam(required = false) UUID skuId,
                                                         @RequestParam(defaultValue = "false") boolean primary) {
        return ApiResponse.success(imageService.upload(productId, skuId, primary, file));
    }

    @PutMapping("/api/v1/admin/catalog/images/{imageId}/primary")
    public ApiResponse<ProductDtos.ImageResponse> makePrimary(@PathVariable UUID imageId) {
        return ApiResponse.success(imageService.makePrimary(imageId));
    }

    @DeleteMapping("/api/v1/admin/catalog/images/{imageId}")
    public ApiResponse<Void> delete(@PathVariable UUID imageId) {
        imageService.delete(imageId);
        return ApiResponse.success(null, "Image deleted");
    }
}
