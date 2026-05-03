package com.smartcommerce.payment.service;

import com.smartcommerce.common.errors.ErrorCode;

/**
 * Maps raw iyzico provider error codes/messages to canonical {@link ErrorCode}
 * values so the API layer and frontend can present user-friendly Turkish copy
 * via i18n keys (errors.PAYMENT_*).
 *
 * <p>iyzico's error codes are exposed as numeric strings ("10003", "10005"…)
 * plus a free-form Turkish errorMessage. We pattern-match on both because the
 * code set is undocumented and shifts between SDK versions.
 */
public final class IyzicoErrorMapper {

    private IyzicoErrorMapper() {}

    public static ErrorCode map(String rawCode, String rawMessage) {
        var code = rawCode == null ? "" : rawCode.trim();
        var msg = rawMessage == null ? "" : rawMessage.toLowerCase(java.util.Locale.ROOT);

        // Banka kartları taksit kabul etmiyor
        if (msg.contains("banka kart") && msg.contains("taksit")) {
            return ErrorCode.PAYMENT_BANK_CARD_NO_INSTALLMENT;
        }
        // Common iyzico installment-not-allowed codes
        if ("10054".equals(code) || "10202".equals(code)) {
            return ErrorCode.PAYMENT_BANK_CARD_NO_INSTALLMENT;
        }

        // Bakiye yetersiz / limit
        if (msg.contains("yetersiz") || msg.contains("limit") || msg.contains("insufficient")) {
            return ErrorCode.PAYMENT_INSUFFICIENT_FUNDS;
        }
        if ("10051".equals(code) || "10005".equals(code)) {
            return ErrorCode.PAYMENT_INSUFFICIENT_FUNDS;
        }

        // 3DS doğrulama başarısız
        if (msg.contains("3d") || msg.contains("doğrulama") || msg.contains("dogrulama")) {
            return ErrorCode.PAYMENT_3DS_FAILED;
        }

        // Reddedildi / declined
        if (msg.contains("reddedildi") || msg.contains("declined") || msg.contains("ret edil")) {
            return ErrorCode.PAYMENT_CARD_DECLINED;
        }
        if ("10012".equals(code) || "10034".equals(code)) {
            return ErrorCode.PAYMENT_CARD_DECLINED;
        }

        // Kart bilgileri hatalı
        if (msg.contains("geçersiz kart") || msg.contains("gecersiz kart")
            || msg.contains("kart numara") || msg.contains("invalid card")
            || msg.contains("son kullan")) {
            return ErrorCode.PAYMENT_INVALID_CARD;
        }
        if ("10003".equals(code) || "10010".equals(code) || "10084".equals(code)) {
            return ErrorCode.PAYMENT_INVALID_CARD;
        }

        return ErrorCode.PAYMENT_GENERIC_FAILURE;
    }
}
