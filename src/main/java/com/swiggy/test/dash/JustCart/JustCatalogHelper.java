package com.swiggy.test.dash.JustCart;

import com.swiggy.dash.imscm.itemcrudbl.v1.ItemHandlerAPIGrpc;
import com.swiggy.dash.imscm.itemcrudbl.v1.ItemHandlerApiProto;
import com.swiggy.test.util.ServiceName;
import com.swiggy.utils.GrpcAllureLoggingInterceptor;
import com.swiggy.utils.grpc.GrpcClientPoolConnection;
import com.swiggy.utils.logger.ILogger;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import java.util.concurrent.TimeUnit;

public class JustCatalogHelper implements ILogger {

    public ItemHandlerApiProto.CreateSpinResponse createSpin(ItemHandlerApiProto.CreateSpinRequest request, String taggedBl) {
        Metadata metadata = new Metadata();
        Metadata.Key<String> taggedBlsKey = Metadata.Key.of("tagged_bls", Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(taggedBlsKey, taggedBl);

        ManagedChannel channel = GrpcClientPoolConnection.getManagedChannelForService(ServiceName.ITEM_HANDLER.toString());
        ItemHandlerAPIGrpc.ItemHandlerAPIBlockingStub stub = ItemHandlerAPIGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata), new GrpcAllureLoggingInterceptor())
                .withDeadlineAfter(10, TimeUnit.SECONDS);

        LOG.info("Creating JUST spin with request: {}", request);
        return stub.createSpin(request);
    }
}
