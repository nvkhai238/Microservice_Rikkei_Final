package com.ecommerce.catalog.image;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** NFR-SEC-010: nhận diện bằng nội dung thật, không tin tên file. */
class ImageTypeDetectorTest {

    @Test
    void detectsRealImageSignatures() {
        assertThat(ImageTypeDetector.detect(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}))
                .contains(ImageTypeDetector.ImageType.JPEG);
        assertThat(ImageTypeDetector.detect(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}))
                .contains(ImageTypeDetector.ImageType.PNG);
        assertThat(ImageTypeDetector.detect("RIFF0000WEBPVP8 ".getBytes(StandardCharsets.US_ASCII)))
                .contains(ImageTypeDetector.ImageType.WEBP);
    }

    @Test
    void rejectsScriptsDisguisedAsImages() {
        assertThat(ImageTypeDetector.detect("<?php system($_GET[1]);".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(ImageTypeDetector.detect("<svg onload=alert(1)>".getBytes(StandardCharsets.US_ASCII))).isEmpty();
    }

    @Test
    void extensionMustMatchContent() {
        assertThat(ImageTypeDetector.extensionMatches("a.JPG", ImageTypeDetector.ImageType.JPEG)).isTrue();
        assertThat(ImageTypeDetector.extensionMatches("a.png", ImageTypeDetector.ImageType.JPEG)).isFalse();
        assertThat(ImageTypeDetector.extensionMatches("noext", ImageTypeDetector.ImageType.PNG)).isFalse();
    }

    @Test
    void servedFileNamesMustBeServerGenerated() {
        assertThat(ImageService.SAFE_FILE_NAME.matcher("51341f46-d726-4cec-ba3b-57cc4f0701be.png").matches()).isTrue();
        assertThat(ImageService.SAFE_FILE_NAME.matcher("../../etc/passwd").matches()).isFalse();
    }
}
