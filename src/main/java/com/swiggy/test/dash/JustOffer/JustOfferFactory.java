package com.swiggy.test.dash.JustOffer;

import com.swiggy.generated.sources.rng.CampaignResponse;
import com.swiggy.generated.sources.rng.OfferCampaignRequest;
import com.swiggy.generated.sources.rng.OfferResponse;
import com.swiggy.generated.sources.rng.OfferTemplateRequest;
import com.swiggy.utils.logger.ILogger;

public class JustOfferFactory implements ILogger {

    private final JustOfferHelper justOfferHelper;

    private static final String JUST_BUSINESS_LINE = "22";

    public JustOfferFactory() {
        this.justOfferHelper = new JustOfferHelper();
    }

    public String createFlatOffer(int discountAmountInRs, int movInRs, int discountCapInRs, String couponCode, String storeId) throws Exception {
        LOG.info("Creating Just flat offer: {} Rs off, MOV={}, cap={}, coupon={}, store={}",
                discountAmountInRs, movInRs, discountCapInRs, couponCode, storeId);

        OfferCampaignRequest campaignRequest = JustOfferEntity.getFlatOfferCampaignRequest(
                discountAmountInRs, movInRs, discountCapInRs, couponCode);
        CampaignResponse campaignResponse = justOfferHelper.createCampaign(campaignRequest);

        String templateId = campaignResponse.getData().getTemplateIds(0);
        LOG.info("Just flat offer campaign created, templateId :: {}", templateId);

        OfferTemplateRequest templateRequest = JustOfferEntity.getTemplateRequest(templateId, storeId);
        OfferResponse offerResponse = justOfferHelper.createOfferFromTemplate(templateRequest);

        String offerId = offerResponse.getData().getOfferId();
        LOG.info("Just flat offer created successfully, offerId :: {}", offerId);
        return offerId;
    }

    public String createPercentOffer(int discountPercent, int movInRs, int discountCapInRs, String couponCode, String storeId) throws Exception {
        LOG.info("Creating Just percent offer: {}% off, MOV={}, cap={}, coupon={}, store={}",
                discountPercent, movInRs, discountCapInRs, couponCode, storeId);

        OfferCampaignRequest campaignRequest = JustOfferEntity.getPercentOfferCampaignRequest(
                discountPercent, movInRs, discountCapInRs, couponCode);
        CampaignResponse campaignResponse = justOfferHelper.createCampaign(campaignRequest);

        String templateId = campaignResponse.getData().getTemplateIds(0);
        LOG.info("Just offer campaign created, templateId :: {}", templateId);

        OfferTemplateRequest templateRequest = JustOfferEntity.getTemplateRequest(templateId, storeId);
        OfferResponse offerResponse = justOfferHelper.createOfferFromTemplate(templateRequest);

        String offerId = offerResponse.getData().getOfferId();
        LOG.info("Just offer created successfully, offerId :: {}", offerId);
        return offerId;
    }

    public String createFreebieOffer(int movInRs, String couponCode, String freebieSpinId, String storeId) throws Exception {
        LOG.info("Creating Just freebie offer: MOV={}, coupon={}, freebieSpin={}, store={}",
                movInRs, couponCode, freebieSpinId, storeId);

        OfferCampaignRequest campaignRequest = JustOfferEntity.getFreebieOfferCampaignRequest(movInRs, couponCode, freebieSpinId);
        CampaignResponse campaignResponse = justOfferHelper.createCampaign(campaignRequest);

        String templateId = campaignResponse.getData().getTemplateIds(0);
        LOG.info("Just freebie offer campaign created, templateId :: {}", templateId);

        OfferTemplateRequest templateRequest = JustOfferEntity.getTemplateRequest(templateId, storeId);
        OfferResponse offerResponse = justOfferHelper.createOfferFromTemplate(templateRequest);

        String offerId = offerResponse.getData().getOfferId();
        LOG.info("Just freebie offer created successfully, offerId :: {}", offerId);
        return offerId;
    }

    public String createItemFlatOffer(int discountAmountInRs, String spinId, String storeId) throws Exception {
        LOG.info("Creating Just item-level flat offer: {} Rs off, spin={}, store={}", discountAmountInRs, spinId, storeId);

        String[] csvParams = buildItemOfferCsvParams(spinId, storeId, "FLAT", String.valueOf(discountAmountInRs));
        String offerId = justOfferHelper.bulkUploadItemOffer(csvParams);
        LOG.info("Just item flat offer created, offerId :: {}", offerId);
        return offerId;
    }

    public String createItemPercentOffer(int discountPercent, String spinId, String storeId) throws Exception {
        LOG.info("Creating Just item-level percent offer: {}% off, spin={}, store={}", discountPercent, spinId, storeId);

        String[] csvParams = buildItemOfferCsvParams(spinId, storeId, "PERCENT", String.valueOf(discountPercent));
        String offerId = justOfferHelper.bulkUploadItemOffer(csvParams);
        LOG.info("Just item percent offer created, offerId :: {}", offerId);
        return offerId;
    }

    private String[] buildItemOfferCsvParams(String spinId, String storeId, String discountType, String discountValue) {
        return new String[]{
                "1",                    // INDEX
                storeId,                // STORE_ID
                "",                     // CITY_ID
                spinId,                 // SPIN_ID
                discountType,           // DISCOUNT_TYPE
                discountValue,          // DISCOUNT_VALUE
                "2026-12-31 23:59:59",  // VALID_TILL
                "2024-01-25 00:00:00",  // VALID_FROM
                "",                     // CUSTOMER_SEGMENTS
                "",                     // CUSTOMER_BRAND_SEGMENTS
                "",                     // CUSTOMER_CATEGORY_SEGMENTS
                JUST_BUSINESS_LINE,     // BUSINESS_LINE
                "",                     // DAY_OF_THE_WEEK
                "",                     // SLOT_START_TIME
                "",                     // SLOT_END_TIME
                "",                     // REDEMPTION_LIMIT
                "BDPO_REGULAR",         // HIERARCHY_TYPE
                "",                     // VIRTUAL_COMBO_ID
                "0",                    // BRAND_SHARE
                ""                      // REDEMPTION_LIMIT_PER_USER
        };
    }
}
