package com.ecommerce.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.CommonErrorCode;
import com.ecommerce.common.web.ServiceClients;
import com.ecommerce.order.config.OrderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Lấy địa chỉ giao hàng từ identity-service BẰNG TOKEN CỦA KHÁCH (chuyển tiếp header Authorization).
 * identity tự kiểm tra địa chỉ có thuộc người gọi không => order không cần tự làm lại (FR-ORD-003).
 */
@Slf4j
@Component
public class IdentityClient {

    private final RestClient restClient;

    public IdentityClient(RestClient.Builder builder, OrderProperties props) {
        this.restClient = ServiceClients.create(builder, props.clients().identityBaseUrl(),
                props.clients().connectTimeout(), props.clients().readTimeout());
    }

    public AddressSnapshot getAddress(UUID addressId, String bearerToken) {
        try {
            AddressEnvelope response = restClient.get()
                    .uri("/api/v1/users/me/addresses/{id}", addressId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(AddressEnvelope.class);
            if (response == null || response.data() == null) {
                throw AppException.validation("addressId", "NOT_FOUND");
            }
            return response.data();
        } catch (HttpClientErrorException.NotFound ex) {
            throw AppException.validation("addressId", "NOT_FOUND");
        } catch (HttpClientErrorException ex) {
            log.warn("Identity address lookup rejected: {}", ex.getStatusCode());
            throw new AppException(CommonErrorCode.AUTHENTICATION_REQUIRED);
        } catch (ResourceAccessException ex) {
            log.warn("Identity unavailable: {}", ex.getMessage());
            throw new AppException(CommonErrorCode.UPSTREAM_UNAVAILABLE);
        }
    }

    /** Bỏ qua field lạ: service kia thêm field mới không làm hỏng service này (NFR-COMP-001) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressSnapshot(UUID id, String recipientName, String phone, String line1, String ward,
                                  String district, String province, String postalCode) {
    }

    /** Bỏ qua field lạ: service kia thêm field mới không làm hỏng service này (NFR-COMP-001) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AddressEnvelope(String code, AddressSnapshot data) {
    }
}
