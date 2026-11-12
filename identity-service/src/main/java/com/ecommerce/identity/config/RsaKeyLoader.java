package com.ecommerce.identity.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Đọc cặp khóa RSA từ file PEM. Nếu chưa có thì sinh mới và lưu lại (chỉ để dev chạy được ngay;
 * production phải cấp key qua secret manager - NFR-SEC-003).
 * Đường dẫn tương đối được thử ở thư mục hiện tại rồi tới thư mục cha (giống cách đọc .env).
 */
@Slf4j
final class RsaKeyLoader {
    private RsaKeyLoader() {
    }

    static KeyPair loadOrCreate(String privatePath, String publicPath) {
        Path priv = resolve(privatePath);
        Path pub = resolve(publicPath);
        try {
            if (Files.exists(priv) && Files.exists(pub)) {
                return new KeyPair(readPublic(pub), readPrivate(priv));
            }
            log.warn("JWT key files not found ({}), generating a new RSA key pair for development", priv);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            Files.createDirectories(priv.toAbsolutePath().getParent());
            writePem(priv, "PRIVATE KEY", pair.getPrivate().getEncoded());
            writePem(pub, "PUBLIC KEY", pair.getPublic().getEncoded());
            try {
                Files.setPosixFilePermissions(priv, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                // Windows: bỏ qua
            }
            return pair;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Cannot load JWT signing keys", e);
        }
    }

    private static Path resolve(String location) {
        Path path = Path.of(location);
        if (path.isAbsolute() || Files.exists(path)) {
            return path;
        }
        Path parent = Path.of("..").resolve(location);
        if (Files.exists(parent)) {
            return parent;
        }
        // Chưa có file ở đâu cả: tạo ở thư mục gốc project (nơi có mvnw)
        boolean runningInModuleDir = !Files.exists(Path.of("mvnw")) && Files.exists(Path.of("..", "mvnw"));
        return runningInModuleDir ? parent : path;
    }

    private static RSAPrivateKey readPrivate(Path path) throws IOException, GeneralSecurityException {
        byte[] der = decodePem(Files.readString(path));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static RSAPublicKey readPublic(Path path) throws IOException, GeneralSecurityException {
        byte[] der = decodePem(Files.readString(path));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private static byte[] decodePem(String pem) {
        String base64 = pem.replaceAll("-----(BEGIN|END) [A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static void writePem(Path path, String type, byte[] der) throws IOException {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        Files.writeString(path, "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n");
    }
}
