package com.swiggy.integration.tests.Just;

import com.swiggy.dash.imscm.itemcrudbl.v1.ItemCommonsProto;
import com.swiggy.dash.imscm.itemcrudbl.v1.ItemHandlerApiProto;
import com.swiggy.pre_made_catalog_gateway.catalog.v1.SPIN;
import com.swiggy.test.dash.JustCart.JustCatalogFactory;
import com.swiggy.utils.logger.ILogger;
import com.swiggy.utils.test_utils.BaseTest;
import io.grpc.StatusRuntimeException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class JustCatalogTest extends BaseTest implements ILogger {

    private final JustCatalogFactory catalogFactory = new JustCatalogFactory();
    private SPIN baseSpin;

    @Test(description = "Create a base SPIN", priority = 1)
    public void createBaseSpinTest() throws Exception {
        baseSpin = catalogFactory.createBaseSpin();
        Assert.assertNotNull(baseSpin.getId(), "Base SPIN should have an ID");
        LOG.info("Created base SPIN :: {}", baseSpin.getId());
    }

    @Test(description = "Create a sellable SPIN linked to an existing base SPIN", priority = 2, dependsOnMethods = "createBaseSpinTest")
    public void createSellableSpinFromBase() throws Exception {
        SPIN sellableSpin = catalogFactory.createSellableSkuForGivenBaseAndDefaultConversionFactor(baseSpin.getId());
        Assert.assertNotNull(sellableSpin.getId(), "Sellable SPIN should have an ID");
        LOG.info("Created sellable SPIN {} from base SPIN {}", sellableSpin.getId(), baseSpin.getId());
    }

    @Test(description = "Create a case SPIN linked to an existing base SPIN", priority = 3, dependsOnMethods = "createBaseSpinTest")
    public void createCaseSpinFromBase() throws Exception {
        SPIN caseSpin = catalogFactory.createCaseSpinForGivenBaseAndDefaultConversionFactor(baseSpin.getId());
        Assert.assertNotNull(caseSpin.getId(), "Case SPIN should have an ID");
        LOG.info("Created case SPIN {} from base SPIN {}", caseSpin.getId(), baseSpin.getId());
    }

    @Test(description = "Verify CASE SPIN creation fails when conversion_factor=0", priority = 4, dependsOnMethods = "createBaseSpinTest")
    public void caseSpinFailsWithZeroConversionFactor() {
        try {
            catalogFactory.createCaseSpinForGivenBase(baseSpin.getId(), 0);
            Assert.fail("CASE SPIN creation should have failed with conversion_factor=0");
        } catch (StatusRuntimeException e) {
            LOG.info("Expected failure for CASE with conversion_factor=0: {}", e.getMessage());
            Assert.assertTrue(e.getMessage().contains("conversion_factor must be a positive integer"),
                    "Expected conversion_factor validation error but got: " + e.getMessage());
        } catch (Exception e) {
            Assert.fail("Unexpected exception type: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    @Test(description = "Verify SELLABLE_VARIANT creation fails when base_spin_id is missing", priority = 5)
    public void sellableVariantFailsWithoutBaseSpinId() {
        try {
            catalogFactory.createSellableSkuForGivenBase("", 200);
            Assert.fail("SELLABLE_VARIANT creation should have failed without base_spin_id");
        } catch (StatusRuntimeException e) {
            LOG.info("Expected failure for SELLABLE_VARIANT without base_spin_id: {}", e.getMessage());
            Assert.assertTrue(e.getMessage().contains("base_spin_id"),
                    "Expected base_spin_id validation error but got: " + e.getMessage());
        } catch (Exception e) {
            Assert.fail("Unexpected exception type: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    @Test(description = "Verify SPIN creation fails when sellable_type=LOOSE but loose_item_type is missing", priority = 6)
    public void looseSpinFailsWithoutLooseItemType() {
        try {
            Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = catalogFactory.buildLooseSpinAttributes();
            attrs.remove("loose_item_type");
            ItemHandlerApiProto.CreateSpinRequest request = catalogFactory.buildCreateSpinRequest(attrs);
            catalogFactory.fireCreateSpinRpc(request);
            Assert.fail("SPIN creation should have failed without loose_item_type");
        } catch (StatusRuntimeException e) {
            LOG.info("Expected failure for missing loose_item_type: {}", e.getMessage());
            Assert.assertTrue(e.getMessage().contains("sellable_type_attributes is required for LOOSE spins"),
                    "Expected sellable_type_attributes validation error but got: " + e.getMessage());
        }
    }

    @Test(description = "Verify CASE SPIN creation fails when base_spin_id is missing", priority = 7)
    public void caseSpinFailsWithoutBaseSpinId() {
        try {
            Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = catalogFactory.buildLooseSpinAttributes();
            attrs.put("loose_item_type", catalogFactory.attr("LOOSE_ITEM_TYPE_CASE"));
            attrs.put("conversion_factor", catalogFactory.attr("30000"));
            ItemHandlerApiProto.CreateSpinRequest request = catalogFactory.buildCreateSpinRequest(attrs);
            catalogFactory.fireCreateSpinRpc(request);
            Assert.fail("CASE SPIN creation should have failed without base_spin_id");
        } catch (StatusRuntimeException e) {
            LOG.info("Expected failure for CASE without base_spin_id: {}", e.getMessage());
            Assert.assertTrue(e.getMessage().contains("base_spin_id"),
                    "Expected base_spin_id validation error but got: " + e.getMessage());
        }
    }

    @Test(description = "Verify BASE SPIN creation fails when base_spin_id is non-empty", priority = 8)
    public void baseSpinFailsWithNonEmptyBaseSpinId() {
        try {
            Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = catalogFactory.buildLooseSpinAttributes();
            attrs.put("loose_item_type", catalogFactory.attr("LOOSE_ITEM_TYPE_BASE"));
            attrs.put("base_spin_id", catalogFactory.attr("SOME_FAKE_ID"));
            ItemHandlerApiProto.CreateSpinRequest request = catalogFactory.buildCreateSpinRequest(attrs);
            catalogFactory.fireCreateSpinRpc(request);
            Assert.fail("BASE SPIN creation should have failed with non-empty base_spin_id");
        } catch (StatusRuntimeException e) {
            LOG.info("Expected failure for BASE with non-empty base_spin_id: {}", e.getMessage());
            Assert.assertTrue(e.getMessage().contains("base_spin_id must be absent")
                            || e.getMessage().contains("base_spin"),
                    "Expected base_spin_id validation error but got: " + e.getMessage());
        }
    }

    @Test(description = "Verify BASE SPIN creation fails when conversion_factor>0", priority = 9)
    public void baseSpinFailsWithPositiveConversionFactor() {
        try {
            Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = catalogFactory.buildLooseSpinAttributes();
            attrs.put("loose_item_type", catalogFactory.attr("LOOSE_ITEM_TYPE_BASE"));
            attrs.put("conversion_factor", catalogFactory.attr("100"));
            ItemHandlerApiProto.CreateSpinRequest request = catalogFactory.buildCreateSpinRequest(attrs);
            catalogFactory.fireCreateSpinRpc(request);
            Assert.fail("BASE SPIN creation should have failed with conversion_factor>0");
        } catch (StatusRuntimeException e) {
            LOG.info("Expected failure for BASE with conversion_factor>0: {}", e.getMessage());
            Assert.assertTrue(e.getMessage().contains("conversion_factor"),
                    "Expected conversion_factor validation error but got: " + e.getMessage());
        }
    }

    @Test(description = "Verify unknown loose_item_type fails gracefully", priority = 10)
    public void unknownLooseItemTypeFailsGracefully() {
        try {
            Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = catalogFactory.buildLooseSpinAttributes();
            attrs.put("loose_item_type", catalogFactory.attr("UNKNOWN_TYPE"));
            ItemHandlerApiProto.CreateSpinRequest request = catalogFactory.buildCreateSpinRequest(attrs);
            catalogFactory.fireCreateSpinRpc(request);
            Assert.fail("SPIN creation should have failed with unknown loose_item_type");
        } catch (StatusRuntimeException e) {
            LOG.info("Expected failure for unknown loose_item_type: {}", e.getMessage());
        }
    }

    @Test(description = "Verify non-numeric conversion_factor fails gracefully", priority = 11)
    public void nonNumericConversionFactorFailsGracefully() {
        try {
            Map<String, ItemCommonsProto.AttributeUpsertEntity> attrs = catalogFactory.buildLooseSpinAttributes();
            attrs.put("loose_item_type", catalogFactory.attr("LOOSE_ITEM_TYPE_CASE"));
            attrs.put("conversion_factor", catalogFactory.attr("abc"));
            attrs.put("base_spin_id", catalogFactory.attr("SOME_FAKE_ID"));
            ItemHandlerApiProto.CreateSpinRequest request = catalogFactory.buildCreateSpinRequest(attrs);
            catalogFactory.fireCreateSpinRpc(request);
            Assert.fail("SPIN creation should have failed with non-numeric conversion_factor");
        } catch (StatusRuntimeException e) {
            LOG.info("Expected failure for non-numeric conversion_factor: {}", e.getMessage());
            Assert.assertTrue(e.getMessage().contains("ParseFloat") || e.getMessage().contains("invalid syntax"),
                    "Expected parse error for non-numeric conversion_factor but got: " + e.getMessage());
        }
    }
}
