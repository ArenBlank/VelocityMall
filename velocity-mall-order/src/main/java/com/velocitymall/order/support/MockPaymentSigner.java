package com.velocitymall.order.support;

import com.velocitymall.order.model.dto.MockPaymentCallbackDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * HMAC-SHA256 signer for mock third-party callbacks.
 */
@Component
public class MockPaymentSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${payment.mock.secret:velocity-mall-mock-payment-secret}")
    private String secret;

    public String sign(MockPaymentCallbackDTO callback) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(canonicalPayload(callback).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Mock payment signature failed", exception);
        }
    }

    public boolean verify(MockPaymentCallbackDTO callback) {
        if (callback == null || !StringUtils.hasText(callback.getSign())) {
            return false;
        }
        return constantTimeEquals(sign(callback), callback.getSign());
    }

    public String canonicalPayload(MockPaymentCallbackDTO callback) {
        return "transactionType=" + valueOf(callback.getTransactionType())
                + "&orderSn=" + valueOf(callback.getOrderSn())
                + "&requestNo=" + valueOf(callback.getRequestNo())
                + "&tradeNo=" + valueOf(callback.getTradeNo())
                + "&amount=" + normalizeAmount(callback.getAmount())
                + "&payType=" + valueOf(callback.getPayType())
                + "&status=" + valueOf(callback.getStatus())
                + "&timestamp=" + valueOf(callback.getTimestamp())
                + "&nonce=" + valueOf(callback.getNonce());
    }

    private String normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }
}
