package com.swiggy.test.dasherp.JustPostOrderHttp;

import com.google.protobuf.util.JsonFormat;
import com.swiggy.generated.sources.checkout.FetchOrderDetailsResponse;
import com.swiggy.generated.sources.consumer.management.UserSessionInfo;
import com.swiggy.utils.logger.ILogger;

public class JustPostOrderHttpDataManager implements ILogger {

    private final JustPostOrderHttpHelper justPostOrderHttpHelper;

    public JustPostOrderHttpDataManager() {
        this.justPostOrderHttpHelper = new JustPostOrderHttpHelper();
    }

    public FetchOrderDetailsResponse fetchOrderDetails(String orderId, UserSessionInfo userSessionInfo) throws Exception {
        String response = justPostOrderHttpHelper.fetchOrderDetails(orderId, userSessionInfo.getTid(), userSessionInfo.getToken());
        FetchOrderDetailsResponse.Builder builder = FetchOrderDetailsResponse.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(response, builder);
        LOG.info("Fetched Just order details for orderId={}", orderId);
        return builder.build();
    }
}
