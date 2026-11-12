package com.ecommerce.catalog.image;

import java.util.Arrays;
import java.util.Optional;

/**
 * Nhận diện loại ảnh THẬT bằng "magic bytes" ở đầu file (NFR-SEC-010).
 * Không tin Content-Type hay đuôi file client gửi: đổi tên shell.php thành shell.jpg là qua mặt được.
 */
public final class ImageTypeDetector {

    public enum ImageType {
        JPEG("image/jpeg", "jpg"), PNG("image/png", "png"), WEBP("image/webp", "webp");

        public final String mime;
        public final String extension;

        ImageType(String mime, String extension) {
            this.mime = mime;
            this.extension = extension;
        }
    }

    private static final byte[] PNG_SIG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private ImageTypeDetector() {
    }

    public static Optional<ImageType> detect(byte[] head) {
        if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return Optional.of(ImageType.JPEG);
        }
        if (head.length >= 8 && Arrays.equals(Arrays.copyOf(head, 8), PNG_SIG)) {
            return Optional.of(ImageType.PNG);
        }
        if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return Optional.of(ImageType.WEBP);
        }
        return Optional.empty();
    }

    /** Đuôi file client khai báo phải khớp loại thật */
    public static boolean extensionMatches(String originalName, ImageType type) {
        if (originalName == null || !originalName.contains(".")) {
            return false;
        }
        String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        return switch (type) {
            case JPEG -> ext.equals("jpg") || ext.equals("jpeg");
            case PNG -> ext.equals("png");
            case WEBP -> ext.equals("webp");
        };
    }
}
