package com.smartcommerce.payment.service;

import com.smartcommerce.common.errors.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IyzicoErrorMapperTest {

    @Test
    @DisplayName("Banka kartı taksit mesajı → PAYMENT_BANK_CARD_NO_INSTALLMENT")
    void bankCardInstallmentMessage_mapped() {
        assertThat(IyzicoErrorMapper.map("10054", "Banka kartları için taksit yapılamaz"))
            .isEqualTo(ErrorCode.PAYMENT_BANK_CARD_NO_INSTALLMENT);
        assertThat(IyzicoErrorMapper.map(null, "Banka kartlarına taksit yapılamaz"))
            .isEqualTo(ErrorCode.PAYMENT_BANK_CARD_NO_INSTALLMENT);
    }

    @Test
    @DisplayName("Yetersiz bakiye → PAYMENT_INSUFFICIENT_FUNDS")
    void insufficientFunds_mapped() {
        assertThat(IyzicoErrorMapper.map("10051", "Yetersiz bakiye"))
            .isEqualTo(ErrorCode.PAYMENT_INSUFFICIENT_FUNDS);
        assertThat(IyzicoErrorMapper.map(null, "Insufficient funds"))
            .isEqualTo(ErrorCode.PAYMENT_INSUFFICIENT_FUNDS);
    }

    @Test
    @DisplayName("Geçersiz kart → PAYMENT_INVALID_CARD")
    void invalidCard_mapped() {
        assertThat(IyzicoErrorMapper.map("10003", "Geçersiz kart numarası"))
            .isEqualTo(ErrorCode.PAYMENT_INVALID_CARD);
        assertThat(IyzicoErrorMapper.map(null, "Invalid card"))
            .isEqualTo(ErrorCode.PAYMENT_INVALID_CARD);
    }

    @Test
    @DisplayName("3DS doğrulama → PAYMENT_3DS_FAILED")
    void threeDsFailure_mapped() {
        assertThat(IyzicoErrorMapper.map(null, "3D Secure doğrulaması başarısız"))
            .isEqualTo(ErrorCode.PAYMENT_3DS_FAILED);
    }

    @Test
    @DisplayName("Banka reddi → PAYMENT_CARD_DECLINED")
    void declined_mapped() {
        assertThat(IyzicoErrorMapper.map("10012", "Banka reddetti"))
            .isEqualTo(ErrorCode.PAYMENT_CARD_DECLINED);
    }

    @Test
    @DisplayName("Bilinmeyen → PAYMENT_GENERIC_FAILURE")
    void unknown_mapsToGeneric() {
        assertThat(IyzicoErrorMapper.map("99999", "Some unknown error"))
            .isEqualTo(ErrorCode.PAYMENT_GENERIC_FAILURE);
        assertThat(IyzicoErrorMapper.map(null, null))
            .isEqualTo(ErrorCode.PAYMENT_GENERIC_FAILURE);
    }
}
