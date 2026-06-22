package com.swiggy.test.dasherp.JustPostOrderHttp;

import com.swiggy.api.*;
import com.swiggy.test.util.EnvUtil;
import com.swiggy.utils.exceptions.APIException;
import com.swiggy.utils.logger.ILogger;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import java.text.MessageFormat;
import java.util.HashMap;

class JustPostOrderHttpHelper implements ILogger {

    private HTTPHeaders getHeaders(String tid, String token) {
        HTTPHeaders httpHeaders = new HTTPHeaders();
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put(HttpHeaders.CONTENT_TYPE, JustPostOrderHttpConstants.HTTP_HEADERS_CONTENT_TYPE_JSON);
        headerMap.put(JustPostOrderHttpConstants.TID, tid);
        headerMap.put(JustPostOrderHttpConstants.TOKEN, token);
        httpHeaders.setHeaders(headerMap);
        return httpHeaders;
    }

    String fetchOrderDetails(String orderId, String tid, String token) throws Exception {
        HTTPHeaders headers = getHeaders(tid, token);
        HTTPQueryParams queryParams = new HTTPQueryParams();
        queryParams.addParam(JustPostOrderHttpConstants.ORDER_ID_PARAMS, orderId);
        HTTPRequestSpecification specification = new HTTPRequestSpecification(headers, queryParams);
        String baseUrl = "https://" + JustPostOrderHttpConstants.JUST_POST_ORDER_SERVICE_PREFIX
                + EnvUtil.getEnvironmentName()
                + JustPostOrderHttpConstants.JUST_POST_ORDER_SERVICE_SUFFIX;
        HTTPResponseHandlers httpResponseHandlers = getRestClient()
                .createRequest(baseUrl, specification, JustPostOrderHttpEndpoint.FETCH_ORDER_DETAILS)
                .get();
        if (httpResponseHandlers.getHTTPResponse().getStatusCode() == HttpStatus.SC_OK) {
            return httpResponseHandlers.getResponseAsString();
        } else {
            throw new APIException(MessageFormat.format(JustPostOrderHttpConstants.ERROR_MESSAGE,
                    "fetchOrderDetails", httpResponseHandlers.getHTTPResponse().getStatusCode(), httpResponseHandlers.getResponseAsString()));
        }
    }

    private RestClient getRestClient() {
        return new UniRestClient();
    }
}
