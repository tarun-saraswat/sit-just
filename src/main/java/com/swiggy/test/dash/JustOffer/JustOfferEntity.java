package com.swiggy.test.dash.JustOffer;

import api.FreebieItem;
import api.FreebieItems;
import com.swiggy.generated.sources.rng.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;

class JustOfferEntity {

    static OfferCampaignRequest getPercentOfferCampaignRequest(int discountPercent, int movInRs, int discountCapInRs, String couponCode) {
        int movInPaise = movInRs * 100;
        int discountCapInPaise = discountCapInRs * 100;

        OfferData offerData = OfferData.newBuilder()
                .setMarketplaceId(JustOfferConstants.MARKETPLACE_ID_INT)
                .setMarketplaceCategory(JustOfferConstants.MARKETPLACE_CATEGORY_INT)
                .setMarketplaceBusinessLine(JustOfferConstants.MARKETPLACE_BUSINESS_LINE)
                .setCouponType(JustOfferConstants.COUPON_TYPE)
                .setAppliesOn(0)
                .setUsagePerUserLimit(JustOfferConstants.DEFAULT_USAGE_PER_USER_LIMIT)
                .setCampaignGlobalLimit(JustOfferConstants.DEFAULT_CAMPAIGN_GLOBAL_LIMIT)
                .setCampaignUserLimit(JustOfferConstants.DEFAULT_CAMPAIGN_USER_LIMIT)
                .setDescriptionTemplate(buildDescriptionTemplate())
                .setBenefitType(1)
                .setInstantDiscountValue(buildInstantDiscountValue(2, discountPercent))
                .setDiscountCap(discountCapInPaise)
                .setSharePercentage(buildSharePercentage())
                .setMinCartValue(movInPaise)
                .setValidFrom(buildValidFrom())
                .setValidTill(buildValidTill())
                .setTotalAvailable(0)
                .setTotalPerUser(0)
                .setOfferStoreCtx(OfferStoreCtx.newBuilder().setShowInStress(true).build())
                .setBusinessCtxV2(Business_ctx_v2.newBuilder()
                        .setTypeUrl(JustOfferConstants.BUSINESS_CTX_V2_TYPE_URL)
                        .setValue(JustOfferConstants.BUSINESS_CTX_V2_VALUE)
                        .build())
                .setCouponCode(couponCode)
                .setSelectiveItemBenefit(Selective_item_benefit.newBuilder()
                        .addAllExclusionGroups(Collections.singletonList(JustOfferConstants.EXCLUSION_GROUP))
                        .setMovEvaluation(JustOfferConstants.MOV_EVALUATION)
                        .build())
                .setBenefitLevelUnit(Benefit_level_unit.newBuilder()
                        .setStoreBenefitUnit(JustOfferConstants.BENEFIT_LEVEL_UNIT_STORE)
                        .build())
                .setSuperInCart(false)
                .build();

        return OfferCampaignRequest.newBuilder()
                .setCampaignName(JustOfferConstants.CAMPAIGN_NAME)
                .setCreatedBy(JustOfferConstants.CREATED_BY)
                .setSource(0)
                .addOffersData(offerData)
                .build();
    }

    static OfferCampaignRequest getFlatOfferCampaignRequest(int discountAmountInRs, int movInRs, int discountCapInRs, String couponCode) {
        int movInPaise = movInRs * 100;
        int discountCapInPaise = discountCapInRs * 100;
        int discountInPaise = discountAmountInRs * 100;

        OfferData offerData = OfferData.newBuilder()
                .setMarketplaceId(JustOfferConstants.MARKETPLACE_ID_INT)
                .setMarketplaceCategory(JustOfferConstants.MARKETPLACE_CATEGORY_INT)
                .setMarketplaceBusinessLine(JustOfferConstants.MARKETPLACE_BUSINESS_LINE)
                .setCouponType(JustOfferConstants.COUPON_TYPE)
                .setAppliesOn(0)
                .setUsagePerUserLimit(JustOfferConstants.DEFAULT_USAGE_PER_USER_LIMIT)
                .setCampaignGlobalLimit(JustOfferConstants.DEFAULT_CAMPAIGN_GLOBAL_LIMIT)
                .setCampaignUserLimit(JustOfferConstants.DEFAULT_CAMPAIGN_USER_LIMIT)
                .setDescriptionTemplate(buildDescriptionTemplate())
                .setBenefitType(1)
                .setInstantDiscountValue(buildInstantDiscountValue(1, discountInPaise))
                .setDiscountCap(discountCapInPaise)
                .setSharePercentage(buildSharePercentage())
                .setMinCartValue(movInPaise)
                .setValidFrom(buildValidFrom())
                .setValidTill(buildValidTill())
                .setTotalAvailable(0)
                .setTotalPerUser(0)
                .setOfferStoreCtx(OfferStoreCtx.newBuilder().setShowInStress(true).build())
                .setBusinessCtxV2(Business_ctx_v2.newBuilder()
                        .setTypeUrl(JustOfferConstants.BUSINESS_CTX_V2_TYPE_URL)
                        .setValue(JustOfferConstants.BUSINESS_CTX_V2_VALUE)
                        .build())
                .setCouponCode(couponCode)
                .setSelectiveItemBenefit(Selective_item_benefit.newBuilder()
                        .addAllExclusionGroups(Collections.singletonList(JustOfferConstants.EXCLUSION_GROUP))
                        .setMovEvaluation(JustOfferConstants.MOV_EVALUATION)
                        .build())
                .setBenefitLevelUnit(Benefit_level_unit.newBuilder()
                        .setStoreBenefitUnit(JustOfferConstants.BENEFIT_LEVEL_UNIT_STORE)
                        .build())
                .setSuperInCart(false)
                .build();

        return OfferCampaignRequest.newBuilder()
                .setCampaignName(JustOfferConstants.CAMPAIGN_NAME)
                .setCreatedBy(JustOfferConstants.CREATED_BY)
                .setSource(0)
                .addOffersData(offerData)
                .build();
    }

    static OfferTemplateRequest getTemplateRequest(String templateId, String storeId) {
        long startDate = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
        long endDate = Instant.now().plus(2, ChronoUnit.DAYS).getEpochSecond();

        return OfferTemplateRequest.newBuilder()
                .setTemplateId(templateId)
                .setStoreId(storeId)
                .setStartDate((int) startDate)
                .setEndDate((int) endDate)
                .setSharePercentage(buildSharePercentage())
                .setIngestionSource(JustOfferConstants.INGESTION_SOURCE)
                .setCreatedBy(JustOfferConstants.CREATED_BY)
                .build();
    }

    private static ValidFrom buildValidFrom() {
        long seconds = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
        return ValidFrom.newBuilder().setSeconds((int) seconds).build();
    }

    private static ValidTill buildValidTill() {
        long seconds = Instant.now().plus(2, ChronoUnit.DAYS).getEpochSecond();
        return ValidTill.newBuilder().setSeconds((int) seconds).build();
    }

    private static InstantDiscountValue buildInstantDiscountValue(int discountType, int discountValue) {
        return InstantDiscountValue.newBuilder()
                .setDiscountType(discountType)
                .setDiscountValue(discountValue)
                .build();
    }

    private static SharePercentage buildSharePercentage() {
        return SharePercentage.newBuilder()
                .setSwiggyShare(JustOfferConstants.DEFAULT_SWIGGY_SHARE)
                .setAllianceShare(JustOfferConstants.DEFAULT_ALLIANCE_SHARE)
                .setStoreShare(JustOfferConstants.DEFAULT_STORE_SHARE)
                .build();
    }

    static OfferCampaignRequest getFreebieOfferCampaignRequest(int movInRs, String couponCode, String freebieSpinId) {
        int movInPaise = movInRs * 100;

        FreebieItem freebieItem = FreebieItem.newBuilder().setSpinId(freebieSpinId).build();
        FreebieItems freebieItems = FreebieItems.newBuilder().addItems(freebieItem).build();
        String encodedSkuId = Base64.getEncoder().encodeToString(freebieItems.toByteArray());

        Entity entity = Entity.newBuilder()
                .setTypeUrl(JustOfferConstants.FREEBIE_ENTITY_TYPE_URL)
                .setValue(encodedSkuId)
                .build();
        Universal_discount universalDiscount = Universal_discount.newBuilder()
                .setEntityType(JustOfferConstants.FREEBIE_ENTITY_TYPE)
                .setEntity(entity)
                .build();
        InstantDiscountValue instantDiscountValue = InstantDiscountValue.newBuilder()
                .setDiscountType(JustOfferConstants.FREEBIE_DISCOUNT_TYPE)
                .setUniversalDiscount(universalDiscount)
                .build();

        OfferData offerData = OfferData.newBuilder()
                .setMarketplaceId(JustOfferConstants.MARKETPLACE_ID_INT)
                .setMarketplaceCategory(JustOfferConstants.MARKETPLACE_CATEGORY_INT)
                .setMarketplaceBusinessLine(JustOfferConstants.MARKETPLACE_BUSINESS_LINE)
                .setCouponType("")
                .setAppliesOn(0)
                .setUsagePerUserLimit(100)
                .setCampaignGlobalLimit(1000)
                .setCampaignUserLimit(1000)
                .setDescriptionTemplate(buildDescriptionTemplate())
                .setBenefitType(1)
                .setInstantDiscountValue(instantDiscountValue)
                .setSharePercentage(buildSharePercentage())
                .setMinCartValue(movInPaise)
                .setValidFrom(buildValidFrom())
                .setValidTill(buildValidTill())
                .setTotalAvailable(0)
                .setTotalPerUser(0)
                .setOfferStoreCtx(OfferStoreCtx.newBuilder().setShowInStress(true).build())
                .setBusinessCtxV2(Business_ctx_v2.newBuilder()
                        .setTypeUrl(JustOfferConstants.BUSINESS_CTX_V2_TYPE_URL)
                        .setValue(JustOfferConstants.BUSINESS_CTX_V2_VALUE)
                        .build())
                .setCouponCode(couponCode)
                .setSelectiveItemBenefit(Selective_item_benefit.newBuilder()
                        .addAllExclusionGroups(Collections.singletonList(JustOfferConstants.EXCLUSION_GROUP))
                        .setMovEvaluation(JustOfferConstants.MOV_EVALUATION)
                        .build())
                .setBenefitLevelUnit(Benefit_level_unit.newBuilder()
                        .setStoreBenefitUnit(JustOfferConstants.BENEFIT_LEVEL_UNIT_STORE)
                        .build())
                .setSuperInCart(false)
                .setIsOptIn(false)
                .build();

        return OfferCampaignRequest.newBuilder()
                .setCampaignName(JustOfferConstants.CAMPAIGN_NAME)
                .setCreatedBy(JustOfferConstants.CREATED_BY)
                .setSource(0)
                .addOffersData(offerData)
                .build();
    }

    private static DescriptionTemplate buildDescriptionTemplate() {
        return DescriptionTemplate.newBuilder()
                .setTitle("")
                .setDescription("")
                .addAllTncList(Collections.singletonList(""))
                .setHeader1("")
                .setHeader2("")
                .setHeader3("")
                .setLogoCreativeId("")
                .setCouponSuccessMessage("")
                .build();
    }
}
