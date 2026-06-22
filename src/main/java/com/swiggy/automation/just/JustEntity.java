package com.swiggy.automation.just;

import com.swiggy.automation.imcheckout.internal.CheckoutDto.CartDto;
import com.swiggy.generated.sources.checkout.ConfirmOrderRequest;
import com.swiggy.generated.sources.checkout.OrderRequest;
import com.swiggy.generated.sources.checkout.OrderResponse;
import com.swiggy.generated.sources.checkout.PaymentInfo;
import com.swiggy.generated.sources.checkout.StructureCartResponse;
import com.swiggy.test.dashpricing.PaymentTypes;

class JustEntity {

    private static final String PAYMENT_TYPE = "PRE_PAYMENT";
    private static final String ORDER_CONTEXT = "ORDER_JOB";
    private static final String USER_CONSENT = "PROCEED_WITH_DUPLICATE_ORDER";

    static OrderRequest getDefaultOrderForInstamart(CartDto cartDto) {
        return OrderRequest.newBuilder()
                .setAddressId(cartDto.getCartResponse().getData().getAddressId())
                .setPaymentInfo(getPaymentInfo(cartDto.getCartResponse(), cartDto.getPaymentType()))
                .setPaymentType(PAYMENT_TYPE)
                .setUserConsent(USER_CONSENT)
                .build();
    }

    static ConfirmOrderRequest getConfirmOrderRequest(OrderResponse orderResponse) {
        return ConfirmOrderRequest.newBuilder()
                .setOrderId(getOrderId(orderResponse))
                .setPaymentTransactionId(getPaymentTransactionId(orderResponse))
                .build();
    }

    static String getOrderId(OrderResponse orderResponse) {
        return orderResponse.getData().getOrders(0).getOrderJobs(0).getOrderJobId();
    }

    private static String getPaymentTransactionId(OrderResponse orderResponse) {
        return orderResponse.getData().getOrders(0).getOrderJobs(0).getPaymentInfo(0).getTransactionId();
    }

    private static PaymentInfo getPaymentInfo(StructureCartResponse response, PaymentTypes paymentType) {
        return PaymentInfo.newBuilder()
                .setPaymentType(PAYMENT_TYPE)
                .setTransactionAmount(Double.parseDouble(response.getData().getBill().getToPay()))
                .setOrderContext(ORDER_CONTEXT)
                .setMetadata(paymentType.getMetadata())
                .setPaymentMethod(paymentType.getValue())
                .build();
    }
}
