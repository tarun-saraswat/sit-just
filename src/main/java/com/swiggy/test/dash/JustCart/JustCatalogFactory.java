package com.swiggy.test.dash.JustCart;

import com.swiggy.dash.imscm.itemcrudbl.v1.ItemCommonsProto;
import com.swiggy.dash.imscm.itemcrudbl.v1.ItemHandlerApiProto;
import com.swiggy.nicatalog.factories.NiCatalogDataManager;
import com.swiggy.platform.shared.marketplace.v2.MpContextProto;
import com.swiggy.pre_made_catalog_gateway.catalog.v1.SPIN;
import com.swiggy.utils.logger.ILogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JustCatalogFactory implements ILogger {

    private static final String JUST_BL = "JUST";
    private static final String DEFAULT_TAX_CODE = "a0a79f85-e41e-44f8-a97d-cd9704c1b88b";
    public static final int DEFAULT_CONVERSION_FACTOR_FOR_SELLABLE_SPIN = 100;
    public static final int DEFAULT_CONVERSION_FACTOR_FOR_CASE_SPIN = 50000;

    private final JustCatalogHelper justCatalogHelper;
    private final NiCatalogDataManager niCatalogDataManager;

    public JustCatalogFactory() {
        this.justCatalogHelper = new JustCatalogHelper();
        this.niCatalogDataManager = new NiCatalogDataManager();
    }

    // --- Public: Build request from attributes map ---

    public ItemHandlerApiProto.CreateSpinRequest buildCreateSpinRequest(Map<String, ItemCommonsProto.AttributeUpsertEntity> attributes) {
        Map<String, String> meta = new HashMap<>();
        meta.put("source_system", "local");
        meta.put("updated_by", "test@swiggy.in");
        meta.put("request_id", "automation-" + System.currentTimeMillis());

        MpContextProto.MarketPlaceContext mpContext = MpContextProto.MarketPlaceContext.newBuilder()
                .setMarketplaceId("SWIGGY")
                .setBusinessLineId("INSTAMART")
                .build();

        return ItemHandlerApiProto.CreateSpinRequest.newBuilder()
                .setMarketPlaceContext(mpContext)
                .putAllAttributes(attributes)
                .putAllMeta(meta)
                .build();
    }

    // --- Public: Execute the RPC and return SPIN ---

    public SPIN executeCreateSpin(ItemHandlerApiProto.CreateSpinRequest request) throws Exception {
        ItemHandlerApiProto.CreateSpinResponse response = justCatalogHelper.createSpin(request, JUST_BL);
        LOG.info("CreateSpin response: {}", response);

        String spinId = response.getSpin();
        List<SPIN> spins = niCatalogDataManager.getSpins(Collections.singletonList(spinId));
        if (spins == null || spins.isEmpty()) {
            throw new RuntimeException("No SPIN found for spinId: " + spinId);
        }
        return spins.get(0);
    }

    // --- Public: Fire the RPC without fetching the SPIN (for negative tests that expect failure) ---

    public void fireCreateSpinRpc(ItemHandlerApiProto.CreateSpinRequest request) {
        justCatalogHelper.createSpin(request, JUST_BL);
    }

    // --- Public: Get a mutable base attributes map for tests to tweak ---

    public Map<String, ItemCommonsProto.AttributeUpsertEntity> buildLooseSpinAttributes() {
        Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = new HashMap<>();

        attrs.put("category", attr("ghee"));
        attrs.put("bar_codes", attr(UUID.randomUUID().toString().replace("-", "").substring(0, 13)));
        attrs.put("commission_type", attr("percentage"));
        attrs.put("commission_value", attr("10"));
        attrs.put("SCM_item_type", attr("NORMAL"));
        attrs.put("super_category/L1", attr("Home Decor"));
        attrs.put("category/L2", attr("Furniture"));
        attrs.put("sub-category/L3", attr("Lighting"));
        attrs.put("brand_id", attr("7c457380674101262c560ab7c54c476c3065f1d7"));
        attrs.put("is_margin_percent", attr("No"));
        attrs.put("product name", attr("Final Testing Base - 1"));
        attrs.put("parent product name", attr("PP" + UUID.randomUUID().toString().replace("-", "").substring(0, 20)));
        attrs.put("mrp", attr("50"));
        attrs.put("cost_price", attr("15"));
        attrs.put("on_invoice_margin", attr("base + 10%"));
        attrs.put("total_margin", attr("base + 10%"));
        attrs.put("weight_in_grams", attr("500.5"));
        attrs.put("hsn_code", attr("22021090"));
        attrs.put("tax_code", attr(DEFAULT_TAX_CODE));
        attrs.put("case_size", attr("5"));
        attrs.put("shelf life number", attr("50"));
        attrs.put("whs_inwarding_cutoff", attr("14"));
        attrs.put("inwarding_cutoff", attr("12"));
        attrs.put("sellable shelf life", attr("10"));
        attrs.put("storage_requirement_temperature", attr("(-18 Degrees)"));
        attrs.put("storage_requirement_type", attr("Ambient"));
        attrs.put("photo_shoot_required", attr("Yes"));
        attrs.put("length_in_cm", attr("10.523"));
        attrs.put("width_in_cm", attr("5.567"));
        attrs.put("height_in_cm", attr("11.512"));
        attrs.put("country_of_origin", attr("India"));
        attrs.put("dsd_wh_crossdock", attr("DSD"));
        attrs.put("perishable", attr("No"));
        attrs.put("maintain_selling_mrp_by", attr("Same selling price & M.R.P"));
        attrs.put("max_allowed_quantity", attr("50"));
        attrs.put("sellable_type", attr("SELLABLE_TYPE_LOOSE"));
        attrs.put("applicable_bls", attr("JUST"));
        attrs.put("return_eligibility", attr("false"));
        attrs.put("number_of_rooms", attr("5"));
        attrs.put("style", attr("Sofa"));
        attrs.put("average_rating", attr("3.8"));
        attrs.put("quantity", attr("1"));
        attrs.put("type_of_room", attr("Living room"));
        attrs.put("unit of measure", attr("g"));
        attrs.put("energy_consumption", attr("25.2"));
        attrs.put("volume_in_cc", attr("840.0"));
        attrs.put("temp_sku", attr("Yes"));
        attrs.put("is_digital", attr("false"));
        attrs.put("is_barcode_available", attr("true"));
        attrs.put("vinculum_flow_enabled", attr("yes"));
        attrs.put("category_id", attr("f894178f-8ad8-4e8b-b7b6-f52577ea1b07"));
        attrs.put("rtv_applicable", attr("No"));

        return attrs;
    }

    // --- Convenience: High-level create methods ---

    public SPIN createBaseSpin() throws Exception {
        Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = buildLooseSpinAttributes();
        attrs.put("loose_item_type", attr("LOOSE_ITEM_TYPE_BASE"));
        return executeCreateSpin(buildCreateSpinRequest(attrs));
    }

    public SPIN createSellableSkuForGivenBase(String baseSpinId, int conversionFactor) throws Exception {
        Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = buildLooseSpinAttributes();
        attrs.put("loose_item_type", attr("LOOSE_ITEM_TYPE_SELLABLE_VARIANT"));
        attrs.put("conversion_factor", attr(String.valueOf(conversionFactor)));
        attrs.put("base_spin_id", attr(baseSpinId));
        return executeCreateSpin(buildCreateSpinRequest(attrs));
    }

    public SPIN createSellableSkuForGivenBaseAndDefaultConversionFactor(String baseSpinId) throws Exception {
        return createSellableSkuForGivenBase(baseSpinId, DEFAULT_CONVERSION_FACTOR_FOR_SELLABLE_SPIN);
    }

    public SPIN createCaseSpinForGivenBase(String baseSpinId, int conversionFactor) throws Exception {
        Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = buildLooseSpinAttributes();
        attrs.put("loose_item_type", attr("LOOSE_ITEM_TYPE_CASE"));
        attrs.put("conversion_factor", attr(String.valueOf(conversionFactor)));
        attrs.put("base_spin_id", attr(baseSpinId));
        return executeCreateSpin(buildCreateSpinRequest(attrs));
    }

    public SPIN createCaseSpinForGivenBaseAndDefaultConversionFactor(String baseSpinId) throws Exception {
        return createCaseSpinForGivenBase(baseSpinId, DEFAULT_CONVERSION_FACTOR_FOR_CASE_SPIN);
    }

    // --- Legacy: Home Decor SPIN for JustCartTest ---

    public SPIN createHomeDecorSpinForJust() throws Exception {
        Map<String, ItemCommonsProto.AttributeUpsertEntity> attributes = buildHomeDecorAttributes();
        return executeCreateSpin(buildCreateSpinRequest(attributes));
    }

    private Map<String, ItemCommonsProto.AttributeUpsertEntity> buildHomeDecorAttributes() {
        Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = new HashMap<>();

        attrs.put("SCM_item_type", attr("NORMAL"));
        attrs.put("length_in_cm", attr("21.0"));
        attrs.put("storage_requirement_temperature", attr("(-18 Degrees)"));
        attrs.put("shelf life number", attr("50"));
        attrs.put("temp_sku", attr("Yes"));
        attrs.put("volume_in_cc", attr("840.0"));
        attrs.put("case_size", attr("50"));
        attrs.put("total_margin", attr("0.2"));
        attrs.put("dsd_wh_crossdock", attr("WH - Cold"));
        attrs.put("max_allowed_quantity", attr("300"));
        attrs.put("photo_shoot_required", attr("No"));
        attrs.put("weight_in_grams", attr("2000.0"));
        attrs.put("on_invoice_margin", attr("0.8"));
        attrs.put("width_in_cm", attr("3.0"));
        attrs.put("image_MN", attr("NI_CATALOG/IMAGES/CIW/2025/3/17/a6f68732-37f1-4983-9bdf-34c0a738d0b1_buy-frames-abstract-wall-art-painting-frame-for-living-room-bedroom-and-home-decor-germinate-by-the-atrang-on-ikiru-online-store-5_991x700.jpg"));
        attrs.put("is_margin_percent", attr("Yes"));
        attrs.put("height_in_cm", attr("20.0"));
        attrs.put("whs_inwarding_cutoff", attr("45"));
        attrs.put("cost_price", attr("100"));
        attrs.put("perishable", attr("No"));
        attrs.put("product name", attr("TestPN-12"));
        attrs.put("quantity", attr("20"));
        attrs.put("unit of measure", attr("g"));
        attrs.put("country_of_origin", attr("India"));
        attrs.put("sellable shelf life", attr("30"));
        attrs.put("mrp", attr("110"));
        attrs.put("hsn_code", attr("20049000"));
        attrs.put("brand_id", attr("dd1c97ec4b6a33d378ce306373bea83b19404e3c"));
        attrs.put("category/L2", attr("Furniture"));
        attrs.put("number_of_rooms", attr("2"));
        attrs.put("type_of_room", attr("Bedroom"));
        attrs.put("average_rating", attr("3.5"));
        attrs.put("energy_consumption", attr("5"));
        attrs.put("style", attr("Bed"));
        attrs.put("sub-category/L3", attr("Lighting"));
        attrs.put("tax_code", attr(DEFAULT_TAX_CODE));
        attrs.put("maintain_selling_mrp_by", attr("Same selling price & M.R.P"));
        attrs.put("super_category/L1", attr("Home Decor"));
        attrs.put("inwarding_cutoff", attr("35"));
        attrs.put("storage_requirement_type", attr("Freezer"));
        attrs.put("bar_codes", attr(UUID.randomUUID().toString().replace("-", "").substring(0, 13)));
        attrs.put("parent product name", attr("PP" + UUID.randomUUID().toString().replace("-", "").substring(0, 20)));
        attrs.put("vinculum_flow_enabled", attr("yes"));
        attrs.put("applicable_bls", attr("JUST"));
        attrs.put("category_id", attr("f894178f-8ad8-4e8b-b7b6-f52577ea1b07"));
        attrs.put("commission_type", attr("percentage"));
        attrs.put("commission_value", attr("12"));
        attrs.put("rtv_applicable", attr("No"));
        attrs.put("is_digital", attr("false"));
        attrs.put("is_barcode_available", attr("true"));

        return attrs;
    }

    public ItemCommonsProto.AttributeUpsertEntity attr(String value) {
        return ItemCommonsProto.AttributeUpsertEntity.newBuilder()
                .setActionTypeValue(ItemCommonsProto.ActionType.ACTION_TYPE_ADD_VALUE)
                .setAttributeValueString(value)
                .build();
    }
}
