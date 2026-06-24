package com.swiggy.automation.just;

public enum JustServiceName {

    JUST_CHECKOUT_SERVICE("just-checkout"),
    OFFER_BUILDER("offer-builder"),
    HULK_SERVICE("hulk"),
    ;

    private final String serviceUrl;

    JustServiceName(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Override
    public String toString() {
        return serviceUrl;
    }
}
