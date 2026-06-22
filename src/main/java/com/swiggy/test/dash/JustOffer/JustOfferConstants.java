package com.swiggy.test.dash.JustOffer;

import java.util.Collections;
import java.util.List;

public class JustOfferConstants {
    static final String CAMPAIGN_ENDPOINT = "/v1/campaign";
    static final String TEMPLATE_ENDPOINT = "/v1/offer/template";

    static final String CONTENT_TYPE = "Content-Type";
    static final String APPLICATION_JSON = "application/json";
    static final String MARKETPLACE_ID = "marketplace-id";
    static final String MARKETPLACE_ID_VALUE = "1";
    static final String BUSINESS_LINE = "business-line";
    static final String BUSINESS_LINE_VALUE = "22";
    static final String MARKETPLACE_CATEGORY_HEADER = "marketplace-category";
    static final String MARKETPLACE_CATEGORY_VALUE = "2";
    static final String CLIENT_ID = "client-id";
    static final String CLIENT_ID_VALUE = "super-dash";

    static final int MARKETPLACE_ID_INT = 1;
    static final int MARKETPLACE_CATEGORY_INT = 2;
    static final int MARKETPLACE_BUSINESS_LINE = 22;

    static final String COUPON_TYPE = "GROWTH";
    static final String CAMPAIGN_NAME = "Just Automation Campaign";
    static final String CREATED_BY = "automation@swiggy.in";

    static final String BUSINESS_CTX_V2_TYPE_URL = "type.googleapis.com/api.BusinessCtxV2";
    static final String BUSINESS_CTX_V2_VALUE = "CgASABoAIgAqAA==";

    static final int DEFAULT_USAGE_PER_USER_LIMIT = 10;
    static final int DEFAULT_CAMPAIGN_GLOBAL_LIMIT = 10;
    static final int DEFAULT_CAMPAIGN_USER_LIMIT = 10;
    static final int DEFAULT_DISCOUNT_CAP_IN_PAISE = 100000;

    static final int DEFAULT_SWIGGY_SHARE = 100;
    static final int DEFAULT_ALLIANCE_SHARE = 0;
    static final int DEFAULT_STORE_SHARE = 0;

    static final String EXCLUSION_GROUP = "LRIG";
    static final int MOV_EVALUATION = 1;
    static final int BENEFIT_LEVEL_UNIT_STORE = 2;
    static final int INGESTION_SOURCE = 1;

    static final int FREEBIE_DISCOUNT_TYPE = 4;
    static final String FREEBIE_ENTITY_TYPE = "UNIVERSAL_DISCOUNT_ENTITY_TYPE_FREEBIE_ITEM";
    static final String FREEBIE_ENTITY_TYPE_URL = "type.googleapis.com/api.FreebieItems";

    static final List<String> EMPTY_LIST = Collections.emptyList();
}
