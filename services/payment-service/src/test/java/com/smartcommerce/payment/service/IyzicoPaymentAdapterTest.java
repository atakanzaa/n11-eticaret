package com.smartcommerce.payment.service;

import com.iyzipay.Options;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IyzicoPaymentAdapterTest {

    private final IyzicoPaymentAdapter adapter = new IyzicoPaymentAdapter(new Options());

    @Test
    @DisplayName("Iyzico price is basket total while paidPrice includes shipping")
    void toIyzicoRequest_shippingKeepsPaidPriceSeparateFromBasketPrice() {
        var request = initiateRequest(new BigDecimal("270.00"), List.of(
            new PaymentProvider.BasketItem("offer-1", "Product", "GENERAL", new BigDecimal("250.00"))
        ));

        var iyzicoRequest = adapter.toIyzicoRequest(request);

        assertThat(iyzicoRequest.getPrice()).isEqualByComparingTo("250.00");
        assertThat(iyzicoRequest.getPaidPrice()).isEqualByComparingTo("270.00");
        assertThat(iyzicoRequest.getBasketItems()).hasSize(1);
        assertThat(iyzicoRequest.getBasketItems().getFirst().getPrice()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("Iyzico paidPrice may be below basket price after coupon discount")
    void toIyzicoRequest_couponDiscountKeepsBasketPriceUndiscounted() {
        var request = initiateRequest(new BigDecimal("225.00"), List.of(
            new PaymentProvider.BasketItem("offer-1", "Product", "GENERAL", new BigDecimal("250.00"))
        ));

        var iyzicoRequest = adapter.toIyzicoRequest(request);

        assertThat(iyzicoRequest.getPrice()).isEqualByComparingTo("250.00");
        assertThat(iyzicoRequest.getPaidPrice()).isEqualByComparingTo("225.00");
    }

    @Test
    @DisplayName("Iyzico price is the sum of all basket item prices")
    void toIyzicoRequest_sumsBasketItems() {
        var request = initiateRequest(new BigDecimal("165.00"), List.of(
            new PaymentProvider.BasketItem("offer-1", "Product A", "GENERAL", new BigDecimal("100.00")),
            new PaymentProvider.BasketItem("offer-2", "Product B", "GENERAL", new BigDecimal("65.00"))
        ));

        var iyzicoRequest = adapter.toIyzicoRequest(request);

        assertThat(iyzicoRequest.getPrice()).isEqualByComparingTo("165.00");
        assertThat(iyzicoRequest.getPaidPrice()).isEqualByComparingTo("165.00");
    }

    private PaymentProvider.InitiateRequest initiateRequest(BigDecimal amount,
                                                           List<PaymentProvider.BasketItem> basketItems) {
        var address = new PaymentProvider.AddressInfo(
            "Atakan Yel", "Istanbul", "Turkey", "123 Test Street", "34000");
        return new PaymentProvider.InitiateRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "atakan@example.com",
            "+905555555555",
            null,
            amount,
            "TRY",
            1,
            new PaymentProvider.CardInfo("Atakan Yel", "5528790000000008", "12", "2030", "123"),
            address,
            address,
            basketItems,
            UUID.randomUUID().toString(),
            "127.0.0.1"
        );
    }
}
