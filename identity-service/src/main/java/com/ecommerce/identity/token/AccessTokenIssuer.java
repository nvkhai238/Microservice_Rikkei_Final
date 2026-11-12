package com.ecommerce.identity.token;

import com.ecommerce.identity.config.JwtProperties;
import com.ecommerce.identity.user.User;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Phát Access Token (FR-IAM-010): sub = userId, iat, exp, roles. KHÔNG nhét email/PII vào token
 * vì JWT chỉ được KÝ chứ không MÃ HÓA - ai cầm token cũng đọc được nội dung.
 */
@Component
public class AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final String issuer;
    private final String keyId;

    public AccessTokenIssuer(JwtEncoder encoder, JwtProperties props, RSAKey rsaKey,
                             @Value("${app.security.issuer}") String issuer) {
        this.encoder = encoder;
        this.props = props;
        this.issuer = issuer;
        this.keyId = rsaKey.getKeyID();
    }

    public String issue(User user, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("roles", List.copyOf(user.getRoles()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return props.accessTokenTtl().toSeconds();
    }
}
