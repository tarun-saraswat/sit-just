package com.swiggy.automation.just;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.swiggy.api.*;
import com.swiggy.generated.sources.checkout.*;
import com.swiggy.generated.sources.consumer.management.UserSessionInfo;
import com.swiggy.generated.sources.dash.clearcart.ClearCart;
import com.swiggy.test.dash.ImEnums.ImDevice;

import com.swiggy.test.util.EnvUtil;
import com.swiggy.utils.JsonUtils;
import com.swiggy.utils.exceptions.APIException;
import com.swiggy.utils.logger.ILogger;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import java.text.MessageFormat;
import java.util.HashMap;

import static com.swiggy.automation.just.JustConstants.*;
import static com.swiggy.automation.just.JustServiceName.JUST_CHECKOUT_SERVICE;
import static com.swiggy.automation.just.JustConstants.PathParams.*;
import static com.swiggy.automation.just.JustConstants.QueryParams.*;
import static com.swiggy.automation.just.JustConstants.Endpoints.*;

class JustHelper implements ILogger {
    private String userAgent = null;
    private String versionCode = null;
    private HTTPHeaders additionalHeaders = null;
    private int retries = 5;

    public void setRetries(int retries) {
        this.retries = Math.max(1, retries);
    }
    private static final String ERROR_MESSAGE = "Error in operation {0} with status code {1} and status message is {2}";

    public JustHelper() {
        setDevice(ImDevice.MOBILE_ANDROID);
    }

    private HTTPHeaders getCartHeadersInstamart(final String tid, final String token, final String deviceId) {
        HTTPHeaders httpHeaders = new HTTPHeaders();
        HashMap<String, String> requestHeaders = new HashMap<>(additionalHeaders.getHeaders());
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, HTTP_HEADERS_CONTENT_TYPE_JSON);
        requestHeaders.put(TID, tid);
        requestHeaders.put(TOKEN, token);
        requestHeaders.put(DEVICE_ID, deviceId);
        requestHeaders.put(SID, DEFAULT_SID);
        requestHeaders.put(USER_AGENT, userAgent);
        requestHeaders.put(VERSION_CODE, versionCode);
        httpHeaders.setHeaders(requestHeaders);
        return httpHeaders;
    }

    public void setDevice(ImDevice device) {
        this.userAgent = device.getUserAgent();
        this.versionCode = device.getVersionCode();
        this.additionalHeaders = device.getAdditionalHeaders();
    }

    private HTTPHeaders getOrderHeadersForInstamart(final UserSessionInfo userSessionInfo) {
        HTTPHeaders httpHeaders = new HTTPHeaders();
        HashMap<String, String> hashMap = new HashMap<>(additionalHeaders.getHeaders());
        hashMap.put(HttpHeaders.CONTENT_TYPE, HTTP_HEADERS_CONTENT_TYPE_JSON);
        hashMap.put(TID, userSessionInfo.getTid());
        hashMap.put(TOKEN, userSessionInfo.getToken());
        hashMap.put(USER_AGENT, userAgent);
        hashMap.put(VERSION_CODE_ORDER, versionCode);
        hashMap.put(DEVICE_ID, userSessionInfo.getDeviceId());
        hashMap.put(SID, DEFAULT_SID);
        httpHeaders.setHeaders(hashMap);
        return httpHeaders;
    }

    /**
     * Create cart for Just with pageType=JUST_CART
     *
     * @param structureCartRequest Request for cart
     * @param userSessionInfo      User session info
     * @return Cart response
     */
    StructureCartResponse createCart(StructureCartRequest structureCartRequest, UserSessionInfo userSessionInfo) throws InvalidProtocolBufferException {
        int statusCode = 0;
        StructureCartResponse structureCartResponse;
        String statusMessage = StringUtils.EMPTY;
        HTTPHeaders cartHeaders = getCartHeadersInstamart(userSessionInfo.getTid(), userSessionInfo.getToken(), userSessionInfo.getDeviceId());

        HTTPPathParams httpPathParams = new HTTPPathParams();
        httpPathParams.addParam(SERVICE_LINE_PRM, JUST_SERVICE_LINE);

        HTTPQueryParams httpQueryParams = new HTTPQueryParams();
        httpQueryParams.addParam(PAGE_TYPE, JUST_CART_PAGE_TYPE);

        HTTPHybridParams hybridParams = new HTTPHybridParams();
        hybridParams.setPathParams(httpPathParams);
        hybridParams.setQueryParams(httpQueryParams);

        String cartPayLoad = JsonFormat.printer().print(structureCartRequest);
        HTTPRequestSpecification specs = new HTTPRequestSpecification(cartHeaders, hybridParams, cartPayLoad);
        LOG.info("Cart payLoad for Just :: {}", JsonUtils.convertToSingleLineJson(cartPayLoad));
        HTTPResponseHandlers handlers = null;
        for (int i = 0; i < retries; i++) {
            handlers = getRestClient().createRequest(justCheckoutBaseUrl(), specs, DASH_ADD_TO_CART).post();
            if (handlers.getHTTPResponse().getStatusCode() == HttpStatus.SC_OK) {
                String response = handlers.getHTTPResponse().getBody().getBodyText();
                StructureCartResponse.Builder structureCartResponseBuilder = StructureCartResponse.newBuilder();
                JsonFormat.parser().ignoringUnknownFields().merge(response, structureCartResponseBuilder);
                structureCartResponse = structureCartResponseBuilder.build();
                statusCode = structureCartResponse.getStatusCode();
                statusMessage = structureCartResponse.getStatusMessage();
                if (statusCode == 0) {
                    return structureCartResponse;
                }
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting to retry createCartForJust", e);
            }
        }

        throw new APIException(MessageFormat.format(ERROR_MESSAGE, "createCartForJust", statusCode, statusMessage),
                new Exception(handlers != null ? handlers.getResponseAsString() : "No response available"));
    }

    /**
     * Creates Order For Just
     *
     * @param orderRequest    Order request object
     * @param userSessionInfo UserSessionInfo for which the order is to be created
     * @return OrderResponse
     */
    OrderResponse createOrder(OrderRequest orderRequest, UserSessionInfo userSessionInfo) throws Exception {
        int statusCode = 0;
        String statusMessage = StringUtils.EMPTY;
        HTTPHeaders orderHeaders = getOrderHeadersForInstamart(userSessionInfo);
        HTTPQueryParams queryParams = new HTTPQueryParams();
        queryParams.addParam(CART_TYPE, JUST_CART_TYPE);
        String orderPayLoad = JsonFormat.printer().preservingProtoFieldNames().print(orderRequest);
        LOG.info("Create Order Just payLoad :: {}", orderPayLoad);
        HTTPRequestSpecification specs = new HTTPRequestSpecification(orderHeaders, queryParams, orderPayLoad);
        HTTPResponseHandlers handlers = null;
        String response;
        OrderResponse orderResponse;
        Thread.sleep(2000);
        for (int i = 0; i < retries; i++) {
            handlers = getRestClient().createRequest(justCheckoutBaseUrl(), specs, INSTAMART_CHECKOUT_ORDER).post();
            if (handlers.getHTTPResponse().getStatusCode() == HttpStatus.SC_OK) {
                response = handlers.getHTTPResponse().getBody().getBodyText();
                OrderResponse.Builder orderResponseBuilder = OrderResponse.newBuilder();
                JsonFormat.parser().ignoringUnknownFields().merge(response, orderResponseBuilder);
                orderResponse = orderResponseBuilder.build();
                statusCode = orderResponse.getStatusCode();
                statusMessage = orderResponse.getStatusMessage();
                if (statusCode == 0) {
                    LOG.info("Response for order creation for Just :: {}", response);
                    return orderResponse;
                }
            }
            Thread.sleep(3000);
        }
        throw new APIException(MessageFormat.format(ERROR_MESSAGE, "createOrderForJust", statusCode, statusMessage), new Exception(handlers.getResponseAsString()));
    }

    StructureCartResponse applyCoupon(ApplyCoupon applyCoupon, UserSessionInfo userSessionInfo) throws Exception {
        int statusCode = 0;
        String statusMessage = StringUtils.EMPTY;
        HTTPHeaders cartHeaders = getCartHeadersInstamart(userSessionInfo.getTid(), userSessionInfo.getToken(), userSessionInfo.getDeviceId());

        HTTPQueryParams httpQueryParams = new HTTPQueryParams();
        httpQueryParams.addParam(PAGE_TYPE, JUST_CART_PAGE_TYPE);
        httpQueryParams.addParam(CART_TYPE, JUST_CART_TYPE);

        HTTPHybridParams hybridParams = new HTTPHybridParams();
        hybridParams.setQueryParams(httpQueryParams);

        String payload = JsonFormat.printer().print(applyCoupon);
        LOG.info("Apply coupon payload for Just :: {}", payload);
        HTTPRequestSpecification specs = new HTTPRequestSpecification(cartHeaders, hybridParams, payload);
        HTTPResponseHandlers handlers = null;
        for (int i = 0; i < retries; i++) {
            handlers = getRestClient().createRequest(justCheckoutBaseUrl(), specs, DASH_CREATE_CART_WITH_COUPON_CODE_NEW).post();
            if (handlers.getHTTPResponse().getStatusCode() == HttpStatus.SC_OK) {
                String response = handlers.getHTTPResponse().getBody().getBodyText();
                StructureCartResponse.Builder builder = StructureCartResponse.newBuilder();
                JsonFormat.parser().ignoringUnknownFields().merge(response, builder);
                StructureCartResponse cartResponse = builder.build();
                statusCode = cartResponse.getStatusCode();
                statusMessage = cartResponse.getStatusMessage();
                if (statusCode == 0) {
                    LOG.info("Apply coupon response for Just :: {}", response);
                    return cartResponse;
                }
            }
            Thread.sleep(2000);
        }
        throw new APIException(MessageFormat.format(ERROR_MESSAGE, "applyCouponForJust", statusCode, statusMessage), new Exception(handlers.getResponseAsString()));
    }

    private RestClient getRestClient() {
        return new UniRestClient();
    }

    private String justCheckoutBaseUrl() {
        return getRestClient().createRequest(JUST_CHECKOUT_SERVICE.toString(), new HTTPRequestSpecification(), "").getBaseUrl();
    }

    /**
     * Confirms Order payment For Instamart
     *
     * @param confirmOrderRequest Represents the request object for creating StructureOrder
     * @param userSessionInfo     UserSessionInfo, for which the order is to be created
     * @return OrderResponse An instance of OrderResponse containing the status of the created order
     */
    OrderResponse confirmOrder(UserSessionInfo userSessionInfo, ConfirmOrderRequest confirmOrderRequest) throws Exception {
        int statusCode = 0;
        String statusMessage = StringUtils.EMPTY;
        HTTPHeaders orderHeaders = getOrderHeadersForInstamart(userSessionInfo);
        HTTPQueryParams queryParams = new HTTPQueryParams();
        queryParams.addParam(CART_TYPE, JUST_CART_TYPE);
        String structureCheckoutPayLoad = JsonFormat.printer().preservingProtoFieldNames().print(confirmOrderRequest);
        LOG.info("Create Structure Order Just payLoad :: {}", structureCheckoutPayLoad);
        HTTPRequestSpecification specs = new HTTPRequestSpecification(orderHeaders, queryParams, structureCheckoutPayLoad);
        HTTPResponseHandlers handlers = null;
        String response;
        OrderResponse structureOrderResponse;
        Thread.sleep(2000);
        for (int i = 0; i < retries; i++) {
            handlers = getRestClient().createRequest(justCheckoutBaseUrl(), specs, INSTAMART_CONFIRM_ORDER).post();
            if (handlers.getHTTPResponse().getStatusCode() == HttpStatus.SC_OK) {
                response = handlers.getHTTPResponse().getBody().getBodyText();
                OrderResponse.Builder structuredOrderResponseBuilder = OrderResponse.newBuilder();
                JsonFormat.parser().ignoringUnknownFields().merge(response, structuredOrderResponseBuilder);
                structureOrderResponse = structuredOrderResponseBuilder.build();
                statusCode = structureOrderResponse.getStatusCode();
                statusMessage = structureOrderResponse.getStatusMessage();

                if (statusCode == 0) {
                    LOG.info("Response for Confirm order for Just :: {}", response);
                    return structureOrderResponse;
                }
            }
            Thread.sleep(3000);
        }
        throw new APIException(MessageFormat.format(ERROR_MESSAGE, "createStructureOrderInstamart", statusCode, statusMessage), new Exception(handlers.getResponseAsString()));
    }

    /**
     * This method clears the cart after placing an unstructured order for PUDO, OA.
     *
     * @param userSessionInfo UserSession, for which the cart is to be created
     */
    ClearCart clearCart(final UserSessionInfo userSessionInfo) throws Exception {
        int statusCode = 0;
        String statusMessage = StringUtils.EMPTY;
        HTTPHeaders headers = getClearCartHeaders(userSessionInfo.getTid());
        HTTPQueryParams httpQueryParams = new HTTPQueryParams();
        httpQueryParams.addParam(CART_TYPE, JUST_CART_TYPE);
        HTTPRequestSpecification spec = new HTTPRequestSpecification(headers, httpQueryParams);
        HTTPResponseHandlers handlers = getRestClient().createRequest(justCheckoutBaseUrl(), spec, INSTAMART_CLEAR_CART).post();
        HTTPBody body = handlers.getHTTPResponse().getBody();
        if (handlers.getHTTPResponse().getStatusCode() == HttpStatus.SC_OK) {
            LOG.info("Clear cart response :: {}", body.getBodyText());
            ClearCart.Builder clearCartBuilder = ClearCart.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(handlers.getResponseAsString(), clearCartBuilder);
            ClearCart clearCart = clearCartBuilder.build();
            statusCode = (int) clearCart.getStatusCode();
            statusMessage = clearCart.getStatusMessage().toString();
            if (statusCode != 0) {
                throw new APIException(MessageFormat.format(ERROR_MESSAGE, "ClearCart for Just", statusCode, statusMessage), new Exception(handlers.getResponseAsString()));
            }
            return clearCart;
        } else {
            throw new APIException(MessageFormat.format(ERROR_MESSAGE, "ClearCart for Just", statusCode, statusMessage), new Exception(handlers.getResponseAsString()));
        }
    }

    /**
     * Headers for clear cart request.
     *
     * @param tid tid of the User
     * @return HTTPHeaders
     */
    private HTTPHeaders getClearCartHeaders(final String tid) {
        HTTPHeaders httpHeaders = new HTTPHeaders();
        HashMap<String, String> requestHeaders = new HashMap<>(additionalHeaders.getHeaders());
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, HTTP_HEADERS_CONTENT_TYPE_JSON);
        requestHeaders.put(TID, tid);
        httpHeaders.setHeaders(requestHeaders);
        return httpHeaders;
    }
}
