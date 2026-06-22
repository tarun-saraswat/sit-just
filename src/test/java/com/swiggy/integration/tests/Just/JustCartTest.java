package com.swiggy.integration.tests.Just;

import com.swiggy.automation.imcheckout.internal.CheckoutDto.CartDto;
import com.swiggy.automation.just.JustCheckoutFactory;
import com.swiggy.driverexperience.internal.dlm.DLM;
import com.swiggy.fulfilment_center_management_gateway.shared.v1.FcOperationType;
import com.swiggy.generated.sources.checkout.FetchOrderDetailsResponse;
import com.swiggy.generated.sources.checkout.LeafOrderStatus;
import com.swiggy.generated.sources.cms.Area;
import com.swiggy.generated.sources.cms.City;
import com.swiggy.generated.sources.cms.Sku;
import com.swiggy.generated.sources.cms.Zone;
import com.swiggy.generated.sources.consumer.management.UserSessionInfo;
import com.swiggy.generated.sources.dash.dataservice.CreateAddressResponse;
import com.swiggy.generated.sources.dash.kms.Store;
import com.swiggy.generated.sources.delivery.DeSession;
import com.swiggy.generated.sources.delivery.DeliveryBoy;
import com.swiggy.generated.sources.sit_dash_sf.IgccIssueRequest;
import com.swiggy.generated.sources.sit_dash_sf.IgccIssueResponse;
import com.swiggy.generated.sources.sit_dash_sf.IgccManualResolutionResponse;
import com.swiggy.integration.tests.dash.DashUtil;
import com.swiggy.nicatalog.factories.NiCatalogFactory;
import com.swiggy.pre_made_catalog_gateway.catalog.v1.SPIN;
import com.swiggy.discounting.statemanagers.OffersStateManager;
import com.swiggy.test.dash.DashOrder.OrderManager;
import com.swiggy.test.dash.DashOrder.OrderStateManager;
import com.swiggy.test.dash.ImEnums.CancellationType;
import com.swiggy.test.dash.ImEnums.DispositionType;
import com.swiggy.test.dash.ImEnums.IgccResolution;
import com.swiggy.test.dash.ImEnums.DiscountType;
import com.swiggy.test.dash.ImSku.ImSkuEntities;
import com.swiggy.test.dash.ImSku.ImSkuFactory;
import com.swiggy.test.dash.ImStore.ImStoreFactory;
import com.swiggy.test.dash.JustCart.JustCatalogFactory;
import com.swiggy.test.dash.JustCart.JustValidators;
import com.swiggy.test.dash.JustOffer.JustOfferFactory;
import com.swiggy.test.dasherp.JustPostOrderHttp.JustPostOrderHttpDataManager;
import com.swiggy.test.dasherp.ccservice.CCServiceFactory;
import com.swiggy.test.dasherp.ccservice.CCServiceManager;
import com.swiggy.test.dasherp.constants.InstamartOrderStatus;
import com.swiggy.test.dashpricing.PaymentTypes;
import com.swiggy.test.dashscm.e2e.common.InwardingE2EUtil;
import com.swiggy.utils.logger.ILogger;
import com.swiggy.utils.test_utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JustCartTest extends BaseTest implements ILogger {

    private final ImStoreFactory imStoreFactory;
    private final JustCheckoutFactory justCheckoutFactory;
    private final OrderManager orderManager;
    private final OrderStateManager orderStateManager;
    private final JustPostOrderHttpDataManager justPostOrderHttpDataManager;
    private final CCServiceManager ccServiceManager;
    private final CCServiceFactory ccServiceFactory;
    private final JustOfferFactory justOfferFactory;
    private final JustValidators justValidators;
    private final ImSkuFactory imSkuFactory;
    private final DLM dlm;

    private static final int SKU_PRICE = 300;
    private final List<String> offersCreatedInTest = new ArrayList<>();
    private final List<Sku> bulkSkusToRevert = new ArrayList<>();
    private final List<String> ordersCreatedInMethod = new ArrayList<>();

    private Store store;
    private City city;
    private Area area;
    private Zone zone;
    private UserSessionInfo user;
    private CreateAddressResponse dropLocation;
    private List<Sku> skuList;
    private DeSession deSession;

    public JustCartTest() {
        imStoreFactory = new ImStoreFactory();
        justCheckoutFactory = new JustCheckoutFactory();
        orderManager = new OrderManager();
        orderStateManager = orderManager.getOrderStateManager();
        justPostOrderHttpDataManager = new JustPostOrderHttpDataManager();
        ccServiceManager = new CCServiceManager();
        ccServiceFactory = ccServiceManager.getFactory();
        justOfferFactory = new JustOfferFactory();
        justValidators = new JustValidators();
        imSkuFactory = new ImSkuFactory(new NiCatalogFactory());
        dlm = new DLM();
    }

    private static final int JUST_BUSINESS_LINE = 22;

    @BeforeClass(alwaysRun = true)
    public void disableAllOffers() throws Exception {
        OffersStateManager offersStateManager = new OffersStateManager();
        offersStateManager.disableOffers(JUST_BUSINESS_LINE);
    }

    @BeforeClass(alwaysRun = true, dependsOnMethods = "disableAllOffers")
    public void setup() throws Exception {
        store = imStoreFactory.buildStoreFromKMS(67);
        city = DashUtil.fetchDefaultCity(store);
        area = DashUtil.buildArea(city, store);
        zone = DashUtil.buildZone(city, area);
        user = DashUtil.createUser();
        dropLocation = DashUtil.createAddressNearStore(store, user);
        skuList = Collections.singletonList(getJustSku());
    }

    @BeforeMethod(alwaysRun = true)
    public void createDE() throws Exception {
        DeliveryBoy deliveryBoy = dlm.getDeFactory().createDE(area, city, zone);
        LOG.info("DE created :: {}", deliveryBoy.getDeId());
        deSession = dlm.getDeStateManager().login(deliveryBoy);
    }

    @AfterMethod(alwaysRun = true)
    public void cancelNonTerminalOrders() {
        for (String orderId : ordersCreatedInMethod) {
            try {
                FetchOrderDetailsResponse details = justPostOrderHttpDataManager.fetchOrderDetails(orderId, user);
                LeafOrderStatus status = details.getStatus();
                if (status != LeafOrderStatus.LEAF_ORDER_STATUS_CANCELLED
                        && status != LeafOrderStatus.LEAF_ORDER_STATUS_COMPLETED
                        && status != LeafOrderStatus.LEAF_ORDER_STATUS_FAILED) {
                    LOG.info("Cancelling non-terminal order {} (status: {})", orderId, status);
                    orderStateManager.cancelOrder(orderId, CancellationType.SDC, 0);
                }
            } catch (Exception e) {
                LOG.error("Failed to cancel order {} in cleanup: {}", orderId, e.getMessage());
            }
        }
        ordersCreatedInMethod.clear();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUpOffers() throws Exception {
        if (!offersCreatedInTest.isEmpty()) {
            OffersStateManager offersStateManager = new OffersStateManager();
            for (String offerId : offersCreatedInTest) {
                offersStateManager.disableOffer(offerId, JUST_BUSINESS_LINE);
            }
            offersCreatedInTest.clear();
        }
        if (!bulkSkusToRevert.isEmpty()) {
            try {
                imSkuFactory.changeSkuAttr(bulkSkusToRevert, ImSkuEntities.attrNormalSpin);
            } catch (Exception e) {
                LOG.error("Failed to revert bulk SKUs to normal: {}", e.getMessage());
            } finally {
                bulkSkusToRevert.clear();
            }
        }
    }

    private Sku getJustSku() throws Exception {
        JustCatalogFactory justCatalogFactory = new JustCatalogFactory();
        SPIN spin = justCatalogFactory.createHomeDecorSpinForJust();
        NiCatalogFactory niCatalogFactory = new NiCatalogFactory();
        Sku sku = niCatalogFactory.createSkuForJust(store, spin);
        niCatalogFactory.skuForceDisable(sku, "MANUAL_OVERRIDE", "ENABLED");
        new InwardingE2EUtil().addStoreLayoutAddRackMappingAndInventory(sku, 20000, FcOperationType.FC_OPERATION_TYPE_PICKING);
        return sku;
    }

    private CartDto createJustOrder(List<Sku> skuList, int quantity) throws Exception {
        CartDto cartDto = new CartDto(user, dropLocation, skuList);
        cartDto.setCartType("JUST");
        cartDto.setPaymentType(PaymentTypes.CASH);
        cartDto.setSkuList(skuList, quantity);
        justCheckoutFactory.clearCart(cartDto);
        justCheckoutFactory.createCart(cartDto);
        justCheckoutFactory.createOrder(cartDto);
        ordersCreatedInMethod.add(cartDto.getOrderId());
        return cartDto;
    }

    private CartDto createJustOrder(List<Sku> skuList) throws Exception {
        return createJustOrder(skuList, 3);
    }

    private CartDto createJustOrderWithAppliedCoupon(List<Sku> skuList, String couponCode) throws Exception {
        CartDto cartDto = new CartDto(user, dropLocation, skuList);
        cartDto.setCartType("JUST");
        cartDto.setPaymentType(PaymentTypes.CASH);
        cartDto.setSkuList(skuList, 3);
        justCheckoutFactory.clearCart(cartDto);
        justCheckoutFactory.createCart(cartDto);
        justCheckoutFactory.applyCoupon(cartDto, couponCode);
        justCheckoutFactory.createOrderAfterCoupon(cartDto);
        ordersCreatedInMethod.add(cartDto.getOrderId());
        return cartDto;
    }

    private String createAndDeliverJustOrder() throws Exception {
        CartDto cartDto = createJustOrder(skuList);
        String orderId = cartDto.getOrderId();
        LOG.info("Delivering Just order :: {}", orderId);
        orderStateManager.deliverOrder(orderId, deSession);
        return orderId;
    }

    //Disabled due to being very basic test scenario, gets covered by other tests
    @Test(description = "Create a Just order successfully", enabled = false)
    public void justOrderCreation() throws Exception {
        createJustOrder(skuList);
    }

    @Test(description = "Cancel a Just cash order and validate no refund")
    public void justCashOrderCancellationNoRefund() throws Exception {
        CartDto cartDto = createJustOrder(skuList);

        String orderId = cartDto.getOrderId();
        LOG.info("Created Just order for cancellation :: {}", orderId);

        orderStateManager.cancelOrder(orderId, CancellationType.SDC, 0);

        FetchOrderDetailsResponse orderDetailsResponse = justPostOrderHttpDataManager.fetchOrderDetails(orderId, user);
        double expectedTotalBill = SKU_PRICE * 3;
        justValidators.validateOrderDetails(orderDetailsResponse, orderId,
                LeafOrderStatus.LEAF_ORDER_STATUS_CANCELLED, expectedTotalBill, null);
        LOG.info("Just order {} cancelled, no refund for cash order", orderId);
    }

    @Test(description = "Deliver Just cash order, raise IGCC issue, provide REFUND_RETURN resolution, complete return, validate no refund")
    public void justCashOrderReturnNoRefund() throws Exception {
        String orderId = createAndDeliverJustOrder();

        IgccIssueRequest issueRequest = ccServiceFactory.createIssueRequest(orderId, user, DashUtil.buildSkuMap(1, skuList), DispositionType.BAD_QUALITY_ITEMS);
        IgccIssueResponse issueResponse = ccServiceFactory.createIssue(issueRequest);
        ccServiceManager.validateCreateIgccResponse(issueRequest, issueResponse);

        IgccManualResolutionResponse resolutionResponse = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            resolutionResponse = ccServiceFactory.provideIGCCResolutionCH(IgccResolution.REFUND_RETURN, issueResponse);
            if (resolutionResponse.getDataList().stream().allMatch(IgccManualResolutionResponse.Data::getIsResolved)) break;
            LOG.info("Resolution not resolved yet (attempt {}), retrying in 3s...", attempt + 1);
            Thread.sleep(3000);
        }
        ccServiceManager.validateResolution(null, issueResponse, resolutionResponse);
        ccServiceManager.getValidator().validateGivenResolutionAmount(resolutionResponse, (double) 0);

        orderStateManager.deliverReturnOrder(orderId, deSession);
        LOG.info("Just order {} return completed, no refund for cash", orderId);

        FetchOrderDetailsResponse orderDetailsResponse = justPostOrderHttpDataManager.fetchOrderDetails(orderId, user);
        double expectedTotalBill = SKU_PRICE * 3;
        justValidators.validateOrderDetails(orderDetailsResponse, orderId,
                LeafOrderStatus.LEAF_ORDER_STATUS_COMPLETED, expectedTotalBill, null);
    }

    //Disabled as Just does not support replacement resolution
    @Test(description = "Deliver Just order, raise IGCC issue, provide REPLACEMENT resolution", enabled = false)
    public void justOrderReplacement() throws Exception {
        String orderId = createAndDeliverJustOrder();

        IgccIssueRequest issueRequest = ccServiceFactory.createIssueRequest(orderId, user, DashUtil.buildSkuMap(1, skuList), DispositionType.PACKAGING_ISSUES);
        IgccIssueResponse issueResponse = ccServiceFactory.createIssue(issueRequest);
        ccServiceManager.validateCreateIgccResponse(issueRequest, issueResponse);

        IgccManualResolutionResponse resolutionResponse = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            resolutionResponse = ccServiceFactory.provideIGCCResolutionCH(IgccResolution.REPLACEMENT, issueResponse);
            if (resolutionResponse.getDataList().stream().allMatch(IgccManualResolutionResponse.Data::getIsResolved)) break;
            LOG.info("Resolution not resolved yet (attempt {}), retrying in 3s...", attempt + 1);
            Thread.sleep(3000);
        }
        ccServiceManager.validateResolution(null, issueResponse, resolutionResponse);
        LOG.info("Just order {} replacement order created successfully", orderId);

        FetchOrderDetailsResponse orderDetailsResponse = justPostOrderHttpDataManager.fetchOrderDetails(orderId, user);
        orderManager.getDashOrderValidator().validateOrderDetailsResponseForIgcc(
                orderDetailsResponse, PaymentTypes.CASH.getValue(), orderId, InstamartOrderStatus.DELIVERY_DELIVERED.name());
    }

    @Test(description = "Create a Just cash order with flat offer of 50 Rs applied and validate discount")
    public void justCashOrderWithFlatOffer() throws Exception {
        String couponCode = "JUST_AUTO_FLAT50";
        int discountAmountRs = 50;
        int quantity = 3;

        String offerId = justOfferFactory.createFlatOffer(discountAmountRs, 500, discountAmountRs, couponCode, String.valueOf(store.getId()));
        offersCreatedInTest.add(offerId);
        LOG.info("Created Just flat offer with coupon :: {}, offerId :: {}", couponCode, offerId);

        CartDto cartDto = createJustOrderWithAppliedCoupon(skuList, couponCode);
        LOG.info("Just order created with flat offer applied, orderId :: {}", cartDto.getOrderId());
        orderStateManager.cancelOrder(cartDto.getOrderId(), CancellationType.SDC, 0);

        justValidators.validateCouponApplied(cartDto.getCartResponse(), couponCode, DiscountType.FLAT, discountAmountRs);

        double expectedTotalBill = (SKU_PRICE * quantity) - discountAmountRs;
        FetchOrderDetailsResponse orderDetailsResponse = justPostOrderHttpDataManager.fetchOrderDetails(cartDto.getOrderId(), user);
        justValidators.validateOrderDetails(orderDetailsResponse, cartDto.getOrderId(),
                LeafOrderStatus.LEAF_ORDER_STATUS_CANCELLED, expectedTotalBill, (double) discountAmountRs);
    }

    @Test(description = "Create a Just cash order with percent offer applied and validate discount")
    public void justCashOrderWithPercentOffer() throws Exception {
        String couponCode = "JUST_AUTO_91";
        int discountPercent = 91;
        int quantity = 3;
        int itemTotal = SKU_PRICE * quantity;
        double expectedDiscount = Math.floor((double) itemTotal * discountPercent / 100);

        String offerId = justOfferFactory.createPercentOffer(discountPercent, 500, 1000, couponCode, String.valueOf(store.getId()));
        offersCreatedInTest.add(offerId);
        LOG.info("Created Just percent offer with coupon :: {}, offerId :: {}", couponCode, offerId);

        CartDto cartDto = createJustOrderWithAppliedCoupon(skuList, couponCode);
        LOG.info("Just order created with offer applied, orderId :: {}", cartDto.getOrderId());
        orderStateManager.cancelOrder(cartDto.getOrderId(), CancellationType.SDC, 0);

        justValidators.validateCouponApplied(cartDto.getCartResponse(), couponCode, DiscountType.PERCENTAGE, expectedDiscount);

        double expectedTotalBill = itemTotal - expectedDiscount;
        FetchOrderDetailsResponse orderDetailsResponse = justPostOrderHttpDataManager.fetchOrderDetails(cartDto.getOrderId(), user);
        justValidators.validateOrderDetails(orderDetailsResponse, cartDto.getOrderId(),
                LeafOrderStatus.LEAF_ORDER_STATUS_CANCELLED, expectedTotalBill, expectedDiscount);
    }

    @Test(description = "Create a Just cash order with item-level flat offer and validate item discount")
    public void justCashOrderWithItemFlatOffer() throws Exception {
        int discountAmountRs = 30;
        int quantity = 3;

        Sku offerSku = skuList.get(0);
        String offerId = justOfferFactory.createItemFlatOffer(discountAmountRs, offerSku.getSpin(), String.valueOf(store.getId()));
        offersCreatedInTest.add(offerId);
        LOG.info("Created Just item flat offer, offerId :: {}", offerId);

        CartDto cartDto = createJustOrder(skuList);
        LOG.info("Just order created with item flat offer, orderId :: {}", cartDto.getOrderId());

        double totalDeductions = justValidators.validateItemLevelDiscount(
                cartDto.getCartResponse(), skuList, DiscountType.FLAT, discountAmountRs);

        double expectedTotalBill = (SKU_PRICE * quantity) - totalDeductions;
        FetchOrderDetailsResponse orderDetailsResponse = justPostOrderHttpDataManager.fetchOrderDetails(cartDto.getOrderId(), user);
        justValidators.validateOrderDetails(orderDetailsResponse, cartDto.getOrderId(),
                LeafOrderStatus.LEAF_ORDER_STATUS_CONFIRMED, expectedTotalBill, null);

        orderStateManager.cancelOrder(cartDto.getOrderId(), CancellationType.SDC, 0);
    }

    @Test(description = "Create a Just cash order with item-level percent offer and validate item discount")
    public void justCashOrderWithItemPercentOffer() throws Exception {
        int discountPercent = 10;
        int quantity = 3;

        Sku offerSku = skuList.get(0);
        String offerId = justOfferFactory.createItemPercentOffer(discountPercent, offerSku.getSpin(), String.valueOf(store.getId()));
        offersCreatedInTest.add(offerId);
        LOG.info("Created Just item percent offer, offerId :: {}", offerId);

        CartDto cartDto = createJustOrder(skuList);
        LOG.info("Just order created with item percent offer, orderId :: {}", cartDto.getOrderId());

        double totalDeductions = justValidators.validateItemLevelDiscount(
                cartDto.getCartResponse(), skuList, DiscountType.PERCENTAGE, discountPercent);

        double expectedTotalBill = (SKU_PRICE * quantity) - totalDeductions;
        FetchOrderDetailsResponse orderDetailsResponse = justPostOrderHttpDataManager.fetchOrderDetails(cartDto.getOrderId(), user);
        justValidators.validateOrderDetails(orderDetailsResponse, cartDto.getOrderId(),
                LeafOrderStatus.LEAF_ORDER_STATUS_CONFIRMED, expectedTotalBill, null);

        orderStateManager.cancelOrder(cartDto.getOrderId(), CancellationType.SDC, 0);
    }

    @Test(description = "Create a Just cash order with freebie item offer applied and validate")
    public void justCashOrderWithFreebieOffer() throws Exception {
        String couponCode = "JUST_AUTO_FREEBIE";

        Sku freebieSku = getJustSku();
        Sku freebieSkuWithPrefix = freebieSku.toBuilder()
                .setId("FREEBIE_".concat(freebieSku.getId()))
                .build();
        String offerId = justOfferFactory.createFreebieOffer(5, couponCode, freebieSku.getSpin(), String.valueOf(store.getId()));
        offersCreatedInTest.add(offerId);
        LOG.info("Created Just freebie offer with coupon :: {}, offerId :: {}, freebieSku :: {}",
                couponCode, offerId, freebieSkuWithPrefix.getId());

        Thread.sleep(5000);

        CartDto cartDto = createJustOrderWithAppliedCoupon(skuList, couponCode);
        LOG.info("Just order created with freebie offer applied, orderId :: {}", cartDto.getOrderId());

        Assert.assertEquals(cartDto.getCartResponse().getData().getCoupon().getCoupon(), couponCode,
                "Freebie coupon should be applied on cart");

        orderStateManager.cancelOrder(cartDto.getOrderId(), CancellationType.SDC, 0);
    }

    @Test(description = "Create a Just bulk cash order, validate bulk cart, then cancel")
    public void justBulkCashOrderCreationAndCancellation() throws Exception {
        Sku bulkSku = getJustSku();
        List<Sku> bulkSkuList = Collections.singletonList(bulkSku);

        imSkuFactory.changeSkuAttr(bulkSkuList, ImSkuEntities.attrBulkWeightSpin);
        bulkSkusToRevert.addAll(bulkSkuList);

        CartDto cartDto = createJustOrder(bulkSkuList, 6);
        String orderId = cartDto.getOrderId();
        LOG.info("Created Just bulk order :: {}", orderId);

        FetchOrderDetailsResponse beforeCancelResponse = justPostOrderHttpDataManager.fetchOrderDetails(orderId, user);
        double totalBill = beforeCancelResponse.getTotalBill();
        justValidators.validateOrderDetails(beforeCancelResponse, orderId,
                LeafOrderStatus.LEAF_ORDER_STATUS_CONFIRMED, totalBill, null);

        orderStateManager.cancelOrder(orderId, CancellationType.SDC, 0);

        FetchOrderDetailsResponse afterCancelResponse = justPostOrderHttpDataManager.fetchOrderDetails(orderId, user);
        justValidators.validateOrderDetails(afterCancelResponse, orderId,
                LeafOrderStatus.LEAF_ORDER_STATUS_CANCELLED, totalBill, null);
        LOG.info("Just bulk order {} cancelled successfully", orderId);
    }
}
