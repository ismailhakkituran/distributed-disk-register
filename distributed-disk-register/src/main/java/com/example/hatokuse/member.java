package com.example.hatokuse;

import com.hatokuse.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Member {
    private final int memberId;
    private final int port;
    private final String storageDir;
    private Server server;
    private final Map<Integer, String> messageStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Member(int memberId, int port) {
        this.memberId = memberId;
        this.port = port;
        this.storageDir = "member_" + memberId + "_storage";
        createStorageDirectory();
        loadMessagesFromDisk();
    }

    private void createStorageDirectory() {
        try {
            Files.createDirectories(Paths.get(storageDir));
            System.out.println("[Member-" + memberId + "] Depolama dizini oluşturuldu: " + storageDir);
        } catch (IOException e) {
            System.err.println("[Member-" + memberId + "] Depolama dizini oluşturulamadı: " + e.getMessage());
        }
    }

    private void loadMessagesFromDisk() {
        File dir = new File(storageDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".msg"));
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    int messageId = Integer.parseInt(fileName.replace(".msg", ""));
                    String content = new String(Files.readAllBytes(file.toPath()));
                    messageStore.put(messageId, content);
                } catch (Exception e) {
                    System.err.println("[Member-" + memberId + "] Mesaj yüklenemedi: " + e.getMessage());
                }
            }
            System.out.println("[Member-" + memberId + "] Diskten " + messageStore.size() + " mesaj yüklendi");
        }
    }

    private void saveMessageToDisk(int messageId, String content) {
        try {
            Path filePath = Paths.get(storageDir, messageId + ".msg");
            Files.write(filePath, content.getBytes());
            messageStore.put(messageId, content);
            System.out.println("[Member-" + memberId + "] Mesaj diske kaydedildi: ID=" + messageId);
        } catch (IOException e) {
            System.err.println("[Member-" + memberId + "] Mesaj kaydedilemedi: " + e.getMessage());
        }
    }

    // --- DÜZELTİLEN KISIM: 127.0.0.1 YAPILDI ---
    private void registerToLeader() {
        String leaderHost = "127.0.0.1"; // localhost yerine IP adresi kullanıldı
        int leaderPort = 50051; 

        System.out.println("[Member-" + memberId + "] Lider'e kayıt olunuyor (" + leaderHost + ":" + leaderPort + ")...");

        ManagedChannel channel = ManagedChannelBuilder.forAddress(leaderHost, leaderPort)
                .usePlaintext()
                .build();

        try {
            FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

            RegisterRequest request = RegisterRequest.newBuilder()
                    .setMemberId(memberId)
                    .setHost("127.0.0.1") // Kendimizi de IP olarak tanıtıyoruz
                    .setPort(port)
                    .build();

            RegisterResponse response = stub.registerMember(request);

            if (response.getSuccess()) {
                System.out.println("[Member-" + memberId + "] ✅ Lider'e başarıyla kayıt olundu! Tolerans Seviyesi: " + response.getToleranceLevel());
            } else {
                System.err.println("[Member-" + memberId + "] ❌ Kayıt reddedildi: " + response.getMessage());
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("[Member-" + memberId + "] ❌ Lider'e ulaşılamadı. Lider henüz başlamamış olabilir.");
            System.err.println("Hata Detayı: " + e.getMessage());
            System.exit(1);
        }
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new FamilyServiceImpl())
                .build()
                .start();
        
        System.out.println("[Member-" + memberId + "] Port " + port + " üzerinde başlatıldı");
        
        // Sunucu ayağa kalktıktan hemen sonra Lider'e kayıt oluyoruz
        registerToLeader();

        // Periyodik istatistik yazdırma
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("[Member-" + memberId + "] Toplam mesaj sayısı: " + messageStore.size());
        }, 10, 10, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("[Member-" + memberId + "] Kapatılıyor...");
            Member.this.stop();
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
        scheduler.shutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private class FamilyServiceImpl extends FamilyServiceGrpc.FamilyServiceImplBase {
        @Override
        public void storeMessage(StoreMessageRequest request, StreamObserver<StoreMessageResponse> responseObserver) {
            try {
                saveMessageToDisk(request.getMessageId(), request.getMessageContent());
                
                StoreMessageResponse response = StoreMessageResponse.newBuilder()
                        .setSuccess(true)
                        .setMemberId(memberId)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                StoreMessageResponse response = StoreMessageResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage(e.getMessage())
                        .setMemberId(memberId)
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveMessage(RetrieveMessageRequest request, StreamObserver<RetrieveMessageResponse> responseObserver) {
            String content = messageStore.get(request.getMessageId());
            
            RetrieveMessageResponse.Builder responseBuilder = RetrieveMessageResponse.newBuilder();
            if (content != null) {
                responseBuilder.setSuccess(true).setMessageContent(content);
            } else {
                responseBuilder.setSuccess(false).setErrorMessage("Mesaj bulunamadı");
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void heartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
            HeartbeatResponse response = HeartbeatResponse.newBuilder()
                    .setAlive(true)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void getMemberStats(StatsRequest request, StreamObserver<StatsResponse> responseObserver) {
            StatsResponse response = StatsResponse.newBuilder()
                    .setMemberId(memberId)
                    .setMessageCount(messageStore.size())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Kullanım: java Member <member_id> <port>");
            System.err.println("Örnek: java Member 1 50052");
            System.exit(1);
        }

        int memberId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);

        Member member = new Member(memberId, port);
        member.start();
        member.blockUntilShutdown();
    }
}