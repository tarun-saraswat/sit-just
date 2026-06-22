package com.swiggy.automation.just;

class JustConstants {

    static final String TID = "tid";
    static final String TOKEN = "token";
    static final String DEVICE_ID = "deviceId";
    static final String SID = "sid";
    static final String USER_AGENT = "user-agent";
    static final String VERSION_CODE = "versionCode";
    static final String VERSION_CODE_ORDER = "version-code";
    static final String DEFAULT_SID = "54655576567676767";
    static final String HTTP_HEADERS_CONTENT_TYPE_JSON = "application/json";

    static class PathParams {
        static final String SERVICE_LINE_PRM = "service_line";
        static final String JUST_SERVICE_LINE = "JUST";
    }

    static class QueryParams {
        static final String PAGE_TYPE = "pageType";
        static final String CART_TYPE = "cartType";
        static final String JUST_CART_PAGE_TYPE = "JUST_CART";
        static final String JUST_CART_TYPE = "JUST";
    }

    static class Endpoints {
        static final String DASH_ADD_TO_CART = "/api/v2/view/{service_line}";
        static final String INSTAMART_CHECKOUT_ORDER = "/api/v1/checkout/order";
        static final String INSTAMART_CONFIRM_ORDER = "/api/v1/checkout/confirm/order";
        static final String INSTAMART_CLEAR_CART = "/api/v2/view/clear-cart";
        static final String DASH_CREATE_CART_WITH_COUPON_CODE_NEW = "/api/v2/view/apply-coupon";
    }
}
