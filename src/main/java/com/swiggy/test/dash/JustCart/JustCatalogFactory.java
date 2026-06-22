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

    private final JustCatalogHelper justCatalogHelper;
    private final NiCatalogDataManager niCatalogDataManager;

    public JustCatalogFactory() {
        this.justCatalogHelper = new JustCatalogHelper();
        this.niCatalogDataManager = new NiCatalogDataManager();
    }

    public SPIN createHomeDecorSpinForJust() throws Exception {
        Map<String, ItemCommonsProto.AttributeUpsertEntity> attributes = buildAttributes();
        Map<String, String> meta = new HashMap<>();
        meta.put("source_system", "local");
        meta.put("updated_by", "test@swiggy.in");
        meta.put("request_id", "automation-" + System.currentTimeMillis());

        MpContextProto.MarketPlaceContext mpContext = MpContextProto.MarketPlaceContext.newBuilder()
                .setMarketplaceId("SWIGGY")
                .setBusinessLineId("INSTAMART")
                .build();

        ItemHandlerApiProto.CreateSpinRequest request = ItemHandlerApiProto.CreateSpinRequest.newBuilder()
                .setMarketPlaceContext(mpContext)
                .putAllAttributes(attributes)
                .putAllMeta(meta)
                .build();

        ItemHandlerApiProto.CreateSpinResponse response = justCatalogHelper.createSpin(request, JUST_BL);
        LOG.info("CreateSpin response: {}", response);

        String spinId = response.getSpin();
        List<SPIN> spins = niCatalogDataManager.getSpins(Collections.singletonList(spinId));
        if (spins == null || spins.isEmpty()) {
            throw new RuntimeException("No SPIN found for spinId: " + spinId);
        }
        return spins.get(0);
    }

    private Map<String, ItemCommonsProto.AttributeUpsertEntity> buildAttributes() {
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
        attrs.put("tax_code", attr("a0a79f85-e41e-44f8-a97d-cd9704c1b88b"));
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

    private ItemCommonsProto.AttributeUpsertEntity attr(String value) {
        return ItemCommonsProto.AttributeUpsertEntity.newBuilder()
                .setActionTypeValue(ItemCommonsProto.ActionType.ACTION_TYPE_ADD_VALUE)
                .setAttributeValueString(value)
                .build();
    }
}
