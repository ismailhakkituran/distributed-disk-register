package com.example.family;

import family.MessageId;
import family.StorageServiceGrpc;
import family.StoreResult;
import family.StoredMessage;
import io.grpc.stub.StreamObserver;

public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase {

    private final DiskManager diskManager;

    public StorageServiceImpl(DiskManager diskManager) {
        this.diskManager = diskManager;
    }

    @Override
    public void store(StoredMessage request, StreamObserver<StoreResult> responseObserver) {
        int id = request.getId();
        String text = request.getText();

        System.out.println("💾 gRPC Store İsteği Geldi: ID=" + id);

        boolean isSuccess = diskManager.saveMessage(id, text);

        StoreResult result = StoreResult.newBuilder()
                .setSuccess(isSuccess)
                .build();

        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    @Override
    public void retrieve(MessageId request, StreamObserver<StoredMessage> responseObserver) {
        int id = request.getId();


        String content = diskManager.loadMessage(id);

        StoredMessage.Builder builder = StoredMessage.newBuilder().setId(id);
        if (content != null) {
            builder.setText(content);
        } else {
            builder.setText("NOT_FOUND");
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}