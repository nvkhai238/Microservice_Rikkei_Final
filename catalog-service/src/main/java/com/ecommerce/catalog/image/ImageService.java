package com.ecommerce.catalog.image;

import com.ecommerce.catalog.CatalogErrorCode;
import com.ecommerce.catalog.config.MediaProperties;
import com.ecommerce.catalog.product.ProductDtos;
import com.ecommerce.catalog.product.ProductEvents;
import com.ecommerce.catalog.product.ProductRepository;
import com.ecommerce.catalog.sku.SkuRepository;
import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Upload ảnh an toàn (FR-CAT-009, NFR-SEC-010):
 * kiểm tra dung lượng, magic bytes, đuôi file khớp nội dung; đặt tên file mới bằng UUID (không dùng tên client
 * => không path traversal "../../etc/passwd"); lưu ngoài vùng thực thi.
 */
@Slf4j
@Service
public class ImageService {
    /** Tên file hợp lệ duy nhất mà server từng sinh ra: uuid.ext */
    static final Pattern SAFE_FILE_NAME = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|webp)$");

    private final ProductImageRepository images;
    private final ProductRepository products;
    private final SkuRepository skus;
    private final ProductEvents productEvents;
    private final AuditService audit;
    private final MediaProperties props;
    private final Clock clock;
    private final Path storageDir;

    public ImageService(ProductImageRepository images, ProductRepository products, SkuRepository skus,
                        ProductEvents productEvents, AuditService audit, MediaProperties props, Clock clock)
            throws IOException {
        this.images = images;
        this.products = products;
        this.skus = skus;
        this.productEvents = productEvents;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
        this.storageDir = Path.of(props.storageDir()).toAbsolutePath().normalize();
        Files.createDirectories(storageDir);
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.ImageResponse> list(UUID productId) {
        return images.findByProductIdOrderByPrimaryDescSortOrderAsc(productId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductDtos.ImageResponse upload(UUID productId, UUID skuId, boolean makePrimary, MultipartFile file) {
        var product = products.findById(productId).orElseThrow(() -> AppException.notFound("Product"));
        if (skuId != null && skus.findById(skuId).filter(s -> s.getProductId().equals(productId)).isEmpty()) {
            throw AppException.validation("skuId", "NOT_IN_PRODUCT");
        }
        if (file == null || file.isEmpty()) {
            throw new AppException(CatalogErrorCode.INVALID_IMAGE, "File is empty");
        }
        if (file.getSize() > props.maxSize().toBytes()) {
            throw new AppException(CatalogErrorCode.INVALID_IMAGE, "File exceeds " + props.maxSize());
        }
        long count = images.countByProductId(productId);
        if (count >= props.maxImagesPerProduct()) {
            throw AppException.businessRule("IMAGE_LIMIT_REACHED", "Too many images for this product");
        }
        ImageTypeDetector.ImageType type = detect(file);
        if (!ImageTypeDetector.extensionMatches(file.getOriginalFilename(), type)) {
            throw new AppException(CatalogErrorCode.INVALID_IMAGE, "File extension does not match its content");
        }

        String fileName = UUID.randomUUID() + "." + type.extension;
        Path target = storageDir.resolve(fileName).normalize();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot store image", e);
        }
        // Transaction rollback thì xóa file vừa ghi để không có file "mồ côi"
        deleteFileOnRollback(target);

        ProductImage image = new ProductImage(productId, skuId, fileName, sanitize(file.getOriginalFilename()),
                type.mime, file.getSize(), (int) count, clock.instant());
        boolean primary = makePrimary || count == 0;
        if (primary) {
            images.clearPrimary(productId);
        }
        image.setPrimary(primary);
        images.save(image);
        product.touch(clock.instant());
        productEvents.changed(product, "IMAGE_ADDED");
        audit.record("PRODUCT_IMAGE_ADDED", "PRODUCT", productId, null,
                Map.of("imageId", image.getId(), "fileName", fileName, "primary", primary));
        return toResponse(image);
    }

    @Transactional
    public ProductDtos.ImageResponse makePrimary(UUID imageId) {
        ProductImage image = images.findById(imageId).orElseThrow(() -> AppException.notFound("Image"));
        images.clearPrimary(image.getProductId());
        image.setPrimary(true);
        audit.record("PRODUCT_IMAGE_PRIMARY", "PRODUCT", image.getProductId(), null, Map.of("imageId", imageId));
        return toResponse(image);
    }

    @Transactional
    public void delete(UUID imageId) {
        ProductImage image = images.findById(imageId).orElseThrow(() -> AppException.notFound("Image"));
        images.delete(image);
        audit.record("PRODUCT_IMAGE_DELETED", "PRODUCT", image.getProductId(), Map.of("imageId", imageId), null);
        Path file = storageDir.resolve(image.getFileName()).normalize();
        // Chỉ xóa file thật sau khi DB commit thành công
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    log.warn("Cannot delete image file {}", file, e);
                }
            }
        });
    }

    /** Trả về đường dẫn file để phục vụ GET /api/v1/media/{fileName}; tên lạ => 404. */
    @Transactional(readOnly = true)
    public StoredImage load(String fileName) {
        if (fileName == null || !SAFE_FILE_NAME.matcher(fileName).matches()) {
            throw AppException.notFound("Image");
        }
        ProductImage image = images.findByFileName(fileName).orElseThrow(() -> AppException.notFound("Image"));
        Path path = storageDir.resolve(fileName).normalize();
        if (!path.startsWith(storageDir) || !Files.exists(path)) {
            throw AppException.notFound("Image");
        }
        return new StoredImage(path, image.getContentType());
    }

    public record StoredImage(Path path, String contentType) {
    }

    private ImageTypeDetector.ImageType detect(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(16);
            return ImageTypeDetector.detect(head).orElseThrow(() -> new AppException(CatalogErrorCode.INVALID_IMAGE,
                    "Only JPEG, PNG and WEBP images are allowed"));
        } catch (IOException e) {
            throw new AppException(CatalogErrorCode.INVALID_IMAGE, "Cannot read file");
        }
    }

    private void deleteFileOnRollback(Path target) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException e) {
                        log.warn("Cannot clean up file {}", target, e);
                    }
                }
            }
        });
    }

    private ProductDtos.ImageResponse toResponse(ProductImage i) {
        return new ProductDtos.ImageResponse(i.getId(), props.publicBasePath() + "/" + i.getFileName(), i.getSkuId(),
                i.isPrimary(), i.getContentType(), i.getSizeBytes());
    }

    public String urlOf(String fileName) {
        return fileName == null ? null : props.publicBasePath() + "/" + fileName;
    }

    private static String sanitize(String name) {
        if (name == null) {
            return null;
        }
        String base = name.replaceAll("[\\\\/]", "_").replaceAll("[^\\p{L}\\p{N}._ -]", "");
        return base.length() > 255 ? base.substring(0, 255) : base;
    }
}
