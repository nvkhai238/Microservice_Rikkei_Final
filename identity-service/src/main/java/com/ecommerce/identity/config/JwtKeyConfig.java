package com.ecommerce.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Ký JWT bằng RSA (RS256): bất đối xứng.
 * <ul>
 *   <li>identity giữ PRIVATE key => chỉ identity phát hành được token.</li>
 *   <li>gateway/service khác chỉ có PUBLIC key (qua /.well-known/jwks.json) => verify được nhưng không giả mạo được.</li>
 *   <li>"kid" (key id) trong header token cho biết dùng key nào => xoay vòng key được.</li>
 * </ul>
 */
@Configuration
public class JwtKeyConfig {

    @Bean
    public RSAKey rsaKey(JwtProperties props) throws Exception {
        KeyPair pair = RsaKeyLoader.loadOrCreate(props.privateKeyPath(), props.publicKeyPath());
        RSAKey withoutKid = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .build();
        // kid = thumbprint của public key: cùng key => cùng kid, đổi key => kid đổi
        return new RSAKey.Builder(withoutKid).keyID(withoutKid.computeThumbprint().toString()).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(source);
    }

    /** identity tự verify token bằng public key trong bộ nhớ (không cần gọi JWKS của chính nó). */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey, @Value("${app.security.issuer}") String issuer) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
