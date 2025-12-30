package com.example.family;

import family.MessageId;
import family.StorageServiceGrpc;
import family.StoreResult;
import family.StoredMessage;
import io.grpc.stub.StreamObserver;

public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase {

    private final DiskManager diskManager;

    // Constructor: DiskManager'ı dışarıdan alır
    public StorageServiceImpl(DiskManager diskManager) {
        this.diskManager = diskManager;
    }

    @Override
    public void store(StoredMessage request, StreamObserver<StoreResult> responseObserver) {
        // 1. Gelen mesajdan bilgileri al
        int id = request.getId();
        String text = request.getText();

        System.out.println("💾 gRPC Store İsteği Geldi: ID=" + id);

        // 2. Senin yazdığın DiskManager ile kaydet
        boolean isSuccess = diskManager.saveMessage(id, text);

        // 3. Sonucu gönder
        StoreResult result = StoreResult.newBuilder()
                .setSuccess(isSuccess)
                .build();

        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    @Override
    public void retrieve(MessageId request, StreamObserver<StoredMessage> responseObserver) {
        // 1. İstenen ID'yi al
        int id = request.getId();

        // 2. Diskten oku
        String content = diskManager.loadMessage(id);

        // 3. Mesaj varsa doldur, yoksa boş gönder
        StoredMessage.Builder builder = StoredMessage.newBuilder().setId(id);
        if (content != null) {
            builder.setText(content);
        } else {
            builder.setText("NOT_FOUND"); // Veya boş bırakılabilir
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}