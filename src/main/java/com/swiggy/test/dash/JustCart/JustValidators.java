package com.swiggy.test.dash.JustCart;

import com.swiggy.generated.sources.checkout.BillLineItem;
import com.swiggy.generated.sources.checkout.BilledItems;
import com.swiggy.generated.sources.checkout.FetchOrderDetailsResponse;
import com.swiggy.generated.sources.checkout.LeafOrderStatus;
import com.swiggy.generated.sources.checkout.StructureCartResponse;
import com.swiggy.generated.sources.cms.Sku;
import com.swiggy.test.dash.ImEnums.DiscountType;
import com.swiggy.utils.logger.ILogger;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JustValidators implements ILogger {

    public void validateCouponApplied(StructureCartResponse cartResponse, String couponCode, DiscountType discountType, double expectedDiscount) {
        SoftAssert softAssert = new SoftAssert();

        softAssert.assertEquals(cartResponse.getData().getCoupon().getCoupon(), couponCode,
                "Coupon code should match applied coupon");
        softAssert.assertEquals(Double.parseDouble(cartResponse.getData().getCoupon().getAmount()), expectedDiscount,
                "Coupon discount amount should match expected");

        boolean discountFound = cartResponse.getData().getBill().getDiscountsList().stream()
                .anyMatch(d -> d.getName().equals(couponCode));
        softAssert.assertTrue(discountFound, "Discount list should contain applied coupon: " + couponCode);

        double actualDiscountValue = cartResponse.getData().getBill().getDiscountsList().stream()
                .filter(d -> d.getName().equals(couponCode))
                .mapToDouble(d -> d.getValue())
                .findFirst().orElse(0);
        softAssert.assertEquals(actualDiscountValue, expectedDiscount,
                "Discount value in bill should match expected");

        if (discountType == DiscountType.PERCENTAGE) {
            boolean hasPercentType = cartResponse.getData().getBill().getDiscountsList().stream()
                    .filter(d -> d.getName().equals(couponCode))
                    .anyMatch(d -> d.getCouponType().equals("PERCENT"));
            softAssert.assertTrue(hasPercentType, "Discount couponType should be PERCENT");
        } else if (discountType == DiscountType.FLAT) {
            boolean hasFlatType = cartResponse.getData().getBill().getDiscountsList().stream()
                    .filter(d -> d.getName().equals(couponCode))
                    .anyMatch(d -> d.getCouponType().equals("FLAT"));
            softAssert.assertTrue(hasFlatType, "Discount couponType should be FLAT");
        }

        double itemTotal = Double.parseDouble(cartResponse.getData().getBill().getItemTotal());
        double toPay = Double.parseDouble(cartResponse.getData().getBill().getToPay());
        double expectedToPay = itemTotal - expectedDiscount;
        softAssert.assertEquals(toPay, expectedToPay,
                "toPay should equal itemTotal - discount (" + itemTotal + " - " + expectedDiscount + ")");

        String couponDiscount = cartResponse.getData().getBill().getBillBreakupDetails().getCouponDiscount();
        softAssert.assertEquals(Double.parseDouble(couponDiscount), expectedDiscount,
                "billBreakupDetails.couponDiscount should match expected");

        softAssert.assertAll();
        LOG.info("Coupon {} validated: discount={}, toPay={}, itemTotal={}, type={}",
                couponCode, expectedDiscount, toPay, itemTotal, discountType.getName());
    }

    public void validateOrderDetails(FetchOrderDetailsResponse response, String expectedOrderId,
                                     LeafOrderStatus expectedStatus, double expectedTotalBill,
                                     Double expectedCouponDiscount) {
        SoftAssert softAssert = new SoftAssert();

        softAssert.assertEquals(response.getOrderId(), expectedOrderId, "orderId should match");
        softAssert.assertEquals(response.getStatus(), expectedStatus, "status should match");
        softAssert.assertEquals((double) response.getTotalBill(), expectedTotalBill, "totalBill should match");

        Optional<BillLineItem> amountPaidItem = response.getBillDetail().getPostOrderBillLineItemsList().stream()
                .filter(item -> item.getTitle().getTitle().equals("Amount Paid"))
                .findFirst();
        softAssert.assertTrue(amountPaidItem.isPresent(), "Amount Paid line item should exist");
        if (amountPaidItem.isPresent()) {
            double amountPaid = parseRupeeString(amountPaidItem.get().getAmount().getText());
            softAssert.assertEquals(amountPaid, expectedTotalBill, "Amount Paid should equal totalBill");
        }

        if (expectedCouponDiscount != null) {
            Optional<BillLineItem> couponItem = response.getBillDetail().getBillLineItemsList().stream()
                    .filter(item -> item.getTitle().getTitle().trim().startsWith("Coupon Discount"))
                    .findFirst();
            softAssert.assertTrue(couponItem.isPresent(), "Coupon Discount line item should exist");
            if (couponItem.isPresent()) {
                double actualDiscount = parseRupeeString(couponItem.get().getAmount().getText());
                softAssert.assertEquals(actualDiscount, expectedCouponDiscount, "Coupon Discount should match");
            }
        }

        softAssert.assertAll();
        LOG.info("Order details validated: orderId={}, status={}, totalBill={}", expectedOrderId, expectedStatus, expectedTotalBill);
    }

    public double validateItemLevelDiscount(StructureCartResponse cartResponse, List<Sku> discountedSkus,
                                            DiscountType discountType, int discountValue) {
        Set<String> skuIds = discountedSkus.stream().map(Sku::getId).collect(Collectors.toSet());
        int discountedItemsCount = 0;
        double totalDeductions = 0;

        for (BilledItems billedItem : cartResponse.getData().getBill().getBilledItemsList()) {
            if (skuIds.contains(billedItem.getItem().getItemId())) {
                double basePrice = Double.parseDouble(billedItem.getBill().getItemBasePrice());
                double expectedDiscount;
                if (discountType == DiscountType.PERCENTAGE) {
                    expectedDiscount = basePrice * discountValue / 100;
                } else {
                    expectedDiscount = discountValue;
                }
                double itemPriceAfterOffer = Double.parseDouble(billedItem.getBill().getItemPriceAfterOffer());
                Assert.assertEquals(itemPriceAfterOffer, basePrice - expectedDiscount, 0.01, "Discounted price for item " + billedItem.getItem().getItemId());
                discountedItemsCount++;
                totalDeductions += expectedDiscount * billedItem.getItem().getQuantity();
            }
        }
        Assert.assertEquals(discountedItemsCount, skuIds.size(), "All discounted SKUs should have item-level discount applied");
        LOG.info("Item-level discount validated: type={}, value={}, totalDeductions={}, itemsDiscounted={}",
                discountType.getName(), discountValue, totalDeductions, discountedItemsCount);
        return totalDeductions;
    }

    private double parseRupeeString(String rupeeText) {
        String cleaned = rupeeText.replace("₹", "").replace(",", "").replace("-", "").trim();
        return Double.parseDouble(cleaned);
    }
}
