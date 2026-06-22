package com.swiggy.test.dash.JustOffer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.opencsv.CSVWriter;
import com.swiggy.api.*;
import com.swiggy.generated.sources.rng.CampaignResponse;
import com.swiggy.generated.sources.rng.OfferCampaignRequest;
import com.swiggy.generated.sources.rng.OfferResponse;
import com.swiggy.generated.sources.rng.OfferTemplateRequest;
import com.swiggy.automation.just.JustServiceName;
import com.swiggy.utils.exceptions.APIException;
import com.swiggy.utils.logger.ILogger;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;

class JustOfferHelper implements ILogger {

    private static final String BUSINESS_LINE_JUST = "just";

    private RestClient getRestClient() {
        return new UniRestClient();
    }

    CampaignResponse createCampaign(OfferCampaignRequest request) throws Exception {
        String payload = serializeProtoToJson(request);
        LOG.info("Just offer campaign payload :: {}", payload);

        HTTPRequestSpecification specs = new HTTPRequestSpecification(getHeaders(), payload);
        HTTPResponseHandlers handlers = getRestClient()
                .createRequest(JustServiceName.OFFER_BUILDER.toString(), specs, JustOfferConstants.CAMPAIGN_ENDPOINT)
                .post();

        if (handlers.getHTTPResponse().getStatusCode() != HttpStatus.SC_OK) {
            throw new APIException(MessageFormat.format(
                    "Failed to create Just offer campaign: {0}", handlers.getResponseAsString()));
        }

        String response = handlers.getHTTPResponse().getBody().getBodyText();
        LOG.info("Just offer campaign response :: {}", response);

        CampaignResponse.Builder builder = CampaignResponse.newBuilder();
        parseJsonToProto(response, builder);
        return builder.build();
    }

    OfferResponse createOfferFromTemplate(OfferTemplateRequest request) throws Exception {
        String payload = serializeProtoToJson(request);
        LOG.info("Just offer template payload :: {}", payload);

        HTTPRequestSpecification specs = new HTTPRequestSpecification(getHeaders(), payload);
        HTTPResponseHandlers handlers = getRestClient()
                .createRequest(JustServiceName.OFFER_BUILDER.toString(), specs, JustOfferConstants.TEMPLATE_ENDPOINT)
                .post();

        if (handlers.getHTTPResponse().getStatusCode() != HttpStatus.SC_OK) {
            throw new APIException(MessageFormat.format(
                    "Failed to create Just offer from template: {0}", handlers.getResponseAsString()));
        }

        String response = handlers.getHTTPResponse().getBody().getBodyText();
        LOG.info("Just offer template response :: {}", response);

        OfferResponse.Builder builder = OfferResponse.newBuilder();
        parseJsonToProto(response, builder);
        return builder.build();
    }

    String bulkUploadItemOffer(String[] csvParams) throws Exception {
        File file = createCsvFile(csvParams);
        String reportId = uploadToHulk(file);
        LOG.info("Just item offer Hulk reportId :: {}", reportId);

        for (int i = 0; i < 5; i++) {
            Thread.sleep(5000);
            try {
                return getOperationStatus(reportId);
            } catch (Exception e) {
                LOG.info("Waiting for item offer creation, attempt {} :: {}", i + 1, e.getMessage());
            }
        }
        throw new APIException("Just item offer creation timed out for reportId: " + reportId);
    }

    private File createCsvFile(String[] csvParams) throws IOException {
        String[] headers = {
                "INDEX", "STORE_ID", "CITY_ID", "SPIN_ID", "DISCOUNT_TYPE", "DISCOUNT_VALUE",
                "VALID_TILL", "VALID_FROM", "CUSTOMER_SEGMENTS", "CUSTOMER_BRAND_SEGMENTS",
                "CUSTOMER_CATEGORY_SEGMENTS", "BUSINESS_LINE", "DAY_OF_THE_WEEK",
                "SLOT_START_TIME", "SLOT_END_TIME", "REDEMPTION_LIMIT", "HIERARCHY_TYPE",
                "VIRTUAL_COMBO_ID", "BRAND_SHARE", "REDEMPTION_LIMIT_PER_USER"
        };

        File tempFile = File.createTempFile("just_item_offer_", ".csv");
        try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(fileWriter)) {
            writer.writeNext(headers, false);
            LOG.info("Writing Just item offer CSV row: {}", String.join(",", csvParams));
            writer.writeNext(csvParams, false);
            writer.flush();
        }
        return tempFile;
    }

    private String uploadToHulk(File file) throws IOException {
        LOG.info("Hulk upload path :: {}", file.getPath());
        HTTPRequestSpecification spec = new HTTPRequestSpecification();
        String baseUrl = getRestClient()
                .createRequest(JustServiceName.HULK_SERVICE.toString(), spec, "")
                .getBaseUrl();

        String uploadUrl = baseUrl + "/hulk/upload?type=JUST_STORE_BULK_ITEM_LEVEL_OFFER_V3&createdBy=automation&central=false";
        LOG.info("curl -X POST '{}' \\\n  -H 'X-User-Status: SUPER' \\\n  -F 'file=@{}'", uploadUrl, file.getPath());

        HttpPost post = new HttpPost(uploadUrl);

        FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        multipartEntityBuilder.addPart("file", fileBody);
        post.setEntity(multipartEntityBuilder.build());
        post.setHeader("X-User-Status", "SUPER");

        HttpResponse response = HttpClientBuilder.create().build().execute(post);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new APIException(MessageFormat.format(
                    "Hulk upload failed: status={0}", response.getStatusLine().getStatusCode()));
        }

        InputStream inputStream = response.getEntity().getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String responseBody = sb.toString();
        LOG.info("Hulk upload response :: {}", responseBody);

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"data\"\\s*:\\s*(\\d+)")
                .matcher(responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new APIException("Could not parse reportId from Hulk response: " + responseBody);
    }

    private String getOperationStatus(String reportId) throws Exception {
        HTTPHeaders headers = new HTTPHeaders();
        HashMap<String, String> headersMap = new HashMap<>();
        headersMap.put("Content-Type", "application/json");
        headersMap.put("X-Business-Line-Id", BUSINESS_LINE_JUST);
        headersMap.put("X-Marketplace-Id", "SWIGGY_IN");
        headersMap.put("X-Source", "test");
        headers.setHeaders(headersMap);

        HTTPRequestSpecification spec = new HTTPRequestSpecification(headers);
        String reportEndpoint = "/hulk/bulkoperation/report/" + reportId;
        String reportBaseUrl = getRestClient().createRequest(JustServiceName.HULK_SERVICE.toString(), new HTTPRequestSpecification(), "").getBaseUrl();
        LOG.info("curl -X GET '{}{}' \\\n  -H 'X-Business-Line-Id: {}' \\\n  -H 'X-Marketplace-Id: SWIGGY_IN' \\\n  -H 'X-Source: test'",
                reportBaseUrl, reportEndpoint, BUSINESS_LINE_JUST);

        HTTPResponseHandlers handlers = getRestClient()
                .createRequest(JustServiceName.HULK_SERVICE.toString(), spec, reportEndpoint)
                .get();

        Thread.sleep(2000);
        if (handlers.getHTTPResponse().getStatusCode() == HttpStatus.SC_OK) {
            String response = handlers.getHTTPResponse().getBody().getBodyText();
            String[] data = response.split(",");
            String offerId = data[6];
            LOG.info("Just item offer operation response, offerId :: {}", offerId);
            return offerId;
        } else {
            throw new APIException(MessageFormat.format(
                    "Hulk report poll failed: {0}", handlers.getResponseAsString()));
        }
    }

    private HTTPHeaders getHeaders() {
        HTTPHeaders httpHeaders = new HTTPHeaders();
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put(JustOfferConstants.CONTENT_TYPE, JustOfferConstants.APPLICATION_JSON);
        headerMap.put(JustOfferConstants.MARKETPLACE_ID, JustOfferConstants.MARKETPLACE_ID_VALUE);
        headerMap.put(JustOfferConstants.BUSINESS_LINE, JustOfferConstants.BUSINESS_LINE_VALUE);
        headerMap.put(JustOfferConstants.MARKETPLACE_CATEGORY_HEADER, JustOfferConstants.MARKETPLACE_CATEGORY_VALUE);
        headerMap.put(JustOfferConstants.CLIENT_ID, JustOfferConstants.CLIENT_ID_VALUE);
        httpHeaders.setHeaders(headerMap);
        return httpHeaders;
    }

    private String serializeProtoToJson(Message protoMessage) throws InvalidProtocolBufferException {
        return JsonFormat.printer()
                .preservingProtoFieldNames()
                .includingDefaultValueFields()
                .print(protoMessage);
    }

    private void parseJsonToProto(String json, Message.Builder builder) throws InvalidProtocolBufferException {
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
    }
}
