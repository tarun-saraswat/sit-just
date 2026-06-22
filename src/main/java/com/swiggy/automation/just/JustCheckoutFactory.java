package com.swiggy.automation.just;

import com.google.protobuf.InvalidProtocolBufferException;
import com.swiggy.automation.imcheckout.internal.CheckoutDto.CartDto;

import com.swiggy.automation.imcheckout.internal.ImCheckoutFactory;
import com.swiggy.generated.sources.checkout.ApplyCoupon;
import com.swiggy.generated.sources.checkout.OrderRequest;
import com.swiggy.generated.sources.checkout.OrderResponse;
import com.swiggy.generated.sources.checkout.StructureCartRequest;
import com.swiggy.generated.sources.checkout.StructureCartResponse;
import com.swiggy.generated.sources.consumer.management.UserSessionInfo;
import com.swiggy.generated.sources.dash.clearcart.ClearCart;
import com.swiggy.test.dash.DashCart.ImCartConstants;

import com.swiggy.test.dash.ImEnums.ImDevice;
import com.swiggy.utils.exceptions.APIException;
import org.testng.Assert;

public class JustCheckoutFactory {

    private final JustHelper justHelper;
    private final ImCheckoutFactory imCheckoutFactory;

    public JustCheckoutFactory() {
        this.justHelper = new JustHelper();
        this.imCheckoutFactory = new ImCheckoutFactory();
    }

    public JustCheckoutFactory(ImDevice device) {
        this.justHelper = new JustHelper();
        this.imCheckoutFactory = new ImCheckoutFactory(device);
        setDevice(device);
    }

    public void setDevice(ImDevice device) {
        justHelper.setDevice(device);
        imCheckoutFactory.setDevice(device);
    }

    /**
     * Create cart for Just with pageType=JUST_CART
     *
     * @param cartDto Cart Details required to create cart / place order
     * @return StructureCartResponse
     */
    public StructureCartResponse createCart(CartDto cartDto) throws Exception {
        StructureCartRequest request = ImCheckoutFactory.createCartRequest(cartDto);
        StructureCartResponse cartResponse = justHelper.createCart(request, cartDto.getUserSessionInfo());
        cartDto.setCartRequest(request);
        cartDto.setCartResponse(cartResponse);
        cartDto.setOrderId("");
        return cartResponse;
    }

    /**
     * Creates Order For Just
     *
     * @param cartDto Cart Details required to create cart / place order
     * @return OrderResponse
     */
    public OrderResponse createOrder(CartDto cartDto) throws Exception {
        OrderRequest orderRequest = null;
        OrderResponse orderResponse = OrderResponse.newBuilder().build();
        try {
            if (cartDto.getCartResponse() == null || !cartDto.getOrderId().isEmpty()) {
                cartDto.setCartResponse(createCart(cartDto));
                cartDto.setOrderId("");
            }
            orderRequest = JustEntity.getDefaultOrderForInstamart(cartDto);
            orderResponse = justHelper.createOrder(orderRequest, cartDto.getUserSessionInfo());
        } catch (APIException e) {
            if (e.toString().contains(ImCartConstants.CART_VALUE_UPDATED_ERROR_MESSAGE)) {
                cartDto.setCartResponse(createCart(cartDto));
                cartDto.setOrderId("");
                orderRequest = JustEntity.getDefaultOrderForInstamart(cartDto);
                orderResponse = justHelper.createOrder(orderRequest, cartDto.getUserSessionInfo());
                Assert.assertEquals(orderResponse.getStatusCode(), 0, orderResponse.getStatusMessage());
            } else {
                throw new APIException(e);
            }
        }
        cartDto.setOrderId(JustEntity.getOrderId(orderResponse));
        OrderResponse confirmOrderResponse = justHelper.confirmOrder(cartDto.getUserSessionInfo(), JustEntity.getConfirmOrderRequest(orderResponse));
        cartDto.setOrderResponse(confirmOrderResponse);
        return confirmOrderResponse;
    }

    /**
     * Apply coupon to an existing Just cart
     *
     * @param cartDto    Cart DTO with user session
     * @param couponCode Coupon code to apply
     * @return StructureCartResponse with coupon applied
     */
    public StructureCartResponse applyCoupon(CartDto cartDto, String couponCode) throws Exception {
        ApplyCoupon applyCoupon = ApplyCoupon.newBuilder()
                .setCouponCode(couponCode)
                .build();
        StructureCartResponse response = justHelper.applyCoupon(applyCoupon, cartDto.getUserSessionInfo());
        cartDto.setCartResponse(response);
        return response;
    }

    /**
     * Creates order for Just without recreating the cart on failure.
     * Use after applyCoupon to preserve the applied coupon.
     *
     * @param cartDto Cart DTO with existing cart response
     * @return OrderResponse
     */
    public OrderResponse createOrderAfterCoupon(CartDto cartDto) throws Exception {
        OrderRequest orderRequest = JustEntity.getDefaultOrderForInstamart(cartDto);
        OrderResponse orderResponse = justHelper.createOrder(orderRequest, cartDto.getUserSessionInfo());
        cartDto.setOrderId(JustEntity.getOrderId(orderResponse));
        OrderResponse confirmOrderResponse = justHelper.confirmOrder(cartDto.getUserSessionInfo(), JustEntity.getConfirmOrderRequest(orderResponse));
        cartDto.setOrderResponse(confirmOrderResponse);
        return confirmOrderResponse;
    }

    /**
     * Retrieves the User cart
     *
     * @param userSessionInfo Session of the user
     * @return StructureCartResponse
     */
    public StructureCartResponse getCart(UserSessionInfo userSessionInfo) throws InvalidProtocolBufferException, InterruptedException {
        return imCheckoutFactory.getCart(userSessionInfo);
    }

    /**
     * Clear cart for the given user
     *
     * @param cartDto DTO object containing all test details
     * @return ClearCart
     */
    public ClearCart clearCart(CartDto cartDto) throws Exception {
        cartDto.setCartResponse(null);
        return justHelper.clearCart(cartDto.getUserSessionInfo());
    }

    /**
     * Confirm order for Just
     *
     * @param userSessionInfo Session of the user
     * @param orderResponse   OrderResponse from createOrder
     * @return OrderResponse
     */
    public OrderResponse confirmOrder(UserSessionInfo userSessionInfo, OrderResponse orderResponse) throws Exception {
        return justHelper.confirmOrder(userSessionInfo, JustEntity.getConfirmOrderRequest(orderResponse));
    }
}
