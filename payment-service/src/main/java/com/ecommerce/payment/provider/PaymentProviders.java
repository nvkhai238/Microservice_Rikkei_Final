package com.ecommerce.payment.provider;

import com.ecommerce.common.exception.AppException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Chọn adapter theo mã provider. Spring tự gom mọi bean PaymentProvider vào đây. */
@Component
public class PaymentProviders {
    private final Map<String, PaymentProvider> byCode;

    public PaymentProviders(List<PaymentProvider> providers) {
        this.byCode = providers.stream().collect(Collectors.toMap(PaymentProvider::code, Function.identity()));
    }

    public PaymentProvider get(String code) {
        PaymentProvider provider = byCode.get(code);
        if (provider == null) {
            throw AppException.businessRule("PROVIDER_NOT_SUPPORTED", "Payment provider " + code + " is not configured");
        }
        return provider;
    }

    public boolean supports(String code) {
        return byCode.containsKey(code);
    }
}
