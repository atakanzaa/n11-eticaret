package com.smartcommerce.payment.service;

import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.Payment;
import com.iyzipay.model.PaymentCard;
import com.iyzipay.model.PaymentItem;
import com.iyzipay.model.PaymentChannel;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.model.Refund;
import com.iyzipay.model.RefundReason;
import com.iyzipay.model.ThreedsInitialize;
import com.iyzipay.model.ThreedsPayment;
import com.iyzipay.request.CreatePaymentRequest;
import com.iyzipay.request.CreateRefundRequest;
import com.iyzipay.request.CreateThreedsPaymentRequest;
import com.iyzipay.request.RetrievePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IyzicoPaymentAdapter implements PaymentProvider {

    private final Options iyzicoOptions;

    @Value("${iyzico.callback-url}")
    private String callbackUrl;

    @Override
    public InitiateResult initiate(InitiateRequest request) {
        var iyzicoRequest = new CreatePaymentRequest();
        iyzicoRequest.setLocale(Locale.TR.getValue());
        iyzicoRequest.setConversationId(request.conversationId());
        iyzicoRequest.setPrice(request.amount());
        iyzicoRequest.setPaidPrice(request.amount());
        iyzicoRequest.setCurrency(Currency.TRY.name());
        iyzicoRequest.setInstallment(request.installment() != null ? request.installment() : 1);
        iyzicoRequest.setBasketId(request.orderId().toString());
        iyzicoRequest.setPaymentChannel(PaymentChannel.WEB.name());
        iyzicoRequest.setPaymentGroup(PaymentGroup.PRODUCT.name());
        iyzicoRequest.setCallbackUrl(callbackUrl);

        var paymentCard = new PaymentCard();
        paymentCard.setCardHolderName(request.card().holderName());
        paymentCard.setCardNumber(request.card().number());
        paymentCard.setExpireMonth(request.card().expireMonth());
        paymentCard.setExpireYear(request.card().expireYear());
        paymentCard.setCvc(request.card().cvc());
        paymentCard.setRegisterCard(0);
        iyzicoRequest.setPaymentCard(paymentCard);

        var buyer = new Buyer();
        buyer.setId(request.userId().toString());
        buyer.setName(extractFirstName(request.billingAddress().contactName()));
        buyer.setSurname(extractLastName(request.billingAddress().contactName()));
        buyer.setGsmNumber(request.userPhone());
        buyer.setEmail(request.userEmail());
        buyer.setIdentityNumber(request.userIdentityNumber() != null ? request.userIdentityNumber() : "11111111111");
        buyer.setRegistrationAddress(request.billingAddress().address());
        buyer.setIp(request.userIp() != null ? request.userIp() : "127.0.0.1");
        buyer.setCity(request.billingAddress().city());
        buyer.setCountry(request.billingAddress().country());
        buyer.setZipCode(request.billingAddress().zipCode());
        iyzicoRequest.setBuyer(buyer);

        iyzicoRequest.setShippingAddress(toAddress(request.shippingAddress()));
        iyzicoRequest.setBillingAddress(toAddress(request.billingAddress()));

        var basketItems = request.basketItems().stream().map(item -> {
            var bi = new com.iyzipay.model.BasketItem();
            bi.setId(item.id());
            bi.setName(item.name());
            bi.setCategory1(item.category());
            bi.setItemType(BasketItemType.PHYSICAL.name());
            bi.setPrice(item.price());
            return bi;
        }).toList();
        iyzicoRequest.setBasketItems(basketItems);

        try {
            var response = ThreedsInitialize.create(iyzicoRequest, iyzicoOptions);
            if ("success".equals(response.getStatus())) {
                return new InitiateResult(true, null, response.getHtmlContent(), null, null);
            }
            log.error("Iyzico 3DS initialize failed: errorCode={}, errorMessage={}",
                response.getErrorCode(), response.getErrorMessage());
            return new InitiateResult(false, null, null, response.getErrorCode(), response.getErrorMessage());
        } catch (Exception e) {
            log.error("Iyzico API call failed", e);
            return new InitiateResult(false, null, null, "PROVIDER_EXCEPTION", e.getMessage());
        }
    }

    @Override
    public CaptureResult capture(CaptureRequest request) {
        var iyzicoRequest = new CreateThreedsPaymentRequest();
        iyzicoRequest.setLocale(Locale.TR.getValue());
        iyzicoRequest.setConversationId(request.conversationId());
        iyzicoRequest.setPaymentId(request.providerPaymentId());
        iyzicoRequest.setConversationData(request.conversationData());

        try {
            var response = ThreedsPayment.create(iyzicoRequest, iyzicoOptions);
            if ("success".equals(response.getStatus())) {
                var lastFour = response.getLastFourDigits();
                var paidPrice = response.getPaidPrice();
                String transactionId = null;
                if (response.getPaymentItems() != null && !response.getPaymentItems().isEmpty()) {
                    transactionId = response.getPaymentItems().get(0).getPaymentTransactionId();
                }
                return new CaptureResult(true, response.getPaymentId(), transactionId,
                    paidPrice, lastFour, response.getCardAssociation(), null, null);
            }
            return new CaptureResult(false, null, null, null, null, null,
                response.getErrorCode(), response.getErrorMessage());
        } catch (Exception e) {
            log.error("Iyzico capture failed", e);
            return new CaptureResult(false, null, null, null, null, null,
                "PROVIDER_EXCEPTION", e.getMessage());
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        var iyzicoRequest = new CreateRefundRequest();
        iyzicoRequest.setLocale(Locale.TR.getValue());
        iyzicoRequest.setConversationId(UUID.randomUUID().toString());
        iyzicoRequest.setPaymentTransactionId(request.providerPaymentTransactionId());
        iyzicoRequest.setPrice(request.amount());
        iyzicoRequest.setCurrency(request.currency());
        iyzicoRequest.setIp("127.0.0.1");
        iyzicoRequest.setReason(RefundReason.BUYER_REQUEST);
        iyzicoRequest.setDescription(request.reason());

        try {
            var response = Refund.create(iyzicoRequest, iyzicoOptions);
            if ("success".equals(response.getStatus())) {
                return new RefundResult(true, response.getPaymentId(), null, null);
            }
            return new RefundResult(false, null, response.getErrorCode(), response.getErrorMessage());
        } catch (Exception e) {
            log.error("Iyzico refund failed", e);
            return new RefundResult(false, null, "PROVIDER_EXCEPTION", e.getMessage());
        }
    }

    @Override
    public PaymentDetails getDetails(String providerPaymentId) {
        var request = new RetrievePaymentRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(UUID.randomUUID().toString());
        request.setPaymentId(providerPaymentId);
        request.setPaymentConversationId(providerPaymentId);

        try {
            var response = Payment.retrieve(request, iyzicoOptions);
            if ("success".equals(response.getStatus())) {
                var paid = response.getPaidPrice() != null ? response.getPaidPrice() : BigDecimal.ZERO;
                return new PaymentDetails(response.getPaymentId(), response.getPaymentStatus(), paid);
            }
            return new PaymentDetails(providerPaymentId, response.getStatus(), BigDecimal.ZERO);
        } catch (Exception e) {
            log.error("Iyzico retrieve failed", e);
            return new PaymentDetails(providerPaymentId, "FAILURE", BigDecimal.ZERO);
        }
    }

    private Address toAddress(AddressInfo info) {
        var address = new Address();
        address.setContactName(info.contactName());
        address.setCity(info.city());
        address.setCountry(info.country());
        address.setAddress(info.address());
        address.setZipCode(info.zipCode());
        return address;
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return ".";
        var parts = fullName.trim().split(" ", 2);
        return parts[0];
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) return ".";
        var parts = fullName.trim().split(" ", 2);
        return parts.length > 1 ? parts[1] : ".";
    }
}
