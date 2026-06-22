package com.swiggy.automation.just;

public enum JustServiceName {

    JUST_CHECKOUT_SERVICE("just-checkout"),
    OFFER_BUILDER("offer-builder"),
    HULK_SERVICE("hulk"),
    HULK("hulk"),
    ;

    private final String serviceName;

    JustServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return serviceName;
    }
}
