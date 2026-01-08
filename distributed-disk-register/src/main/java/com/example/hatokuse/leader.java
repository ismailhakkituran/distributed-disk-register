package com.example.hatokuse;

import com.hatokuse.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Leader {
    private final int port;
    private final int clientPort;
    private Server grpcServer;
    private ServerSocket clientServer;
    private int toleranceLevel;
    private final Map<Integer, MemberInfo> members = new ConcurrentHashMap<>();
    private final Map<Integer, List<Integer>> messageToMembersMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private int nextMemberIndex = 0;
    private final String storageDir = "leader_storage";

    static class MemberInfo {
        int memberId;
        String host;
        int port;
        ManagedChannel channel;
        FamilyServiceGrpc.FamilyServiceBlockingStub stub;
        boolean isAlive = true;

        MemberInfo(int memberId, String host, int port) {
            this.memberId = memberId;
            this.host = host;
            this.port = port;
            // Kanal oluştururken hata olursa yakalamak için burada da işlem yapıyoruz
            try {
                this.channel = ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .build();
                this.stub = FamilyServiceGrpc.newBlockingStub(channel);
            } catch (Exception e) {
                System.err.println("[Leader] HATA: Member-" + memberId + " için kanal oluşturulamadı: " + e.getMessage());
                throw e; // Hatayı yukarı fırlat ki Lider bilsin
            }
        }

        void shutdown() {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdown();
            }
        }
    }

    public Leader(int port, int clientPort) {
        this.port = port;
        this.clientPort = clientPort;
        loadToleranceConfig();
        createStorageDirectory();
        loadMessageMapping();
    }

    private void createStorageDirectory() {
        try {
            Files.createDirectories(Paths.get(storageDir));
            System.out.println("[Leader] Depolama dizini oluşturuldu: " + storageDir);
        } catch (IOException e) {
            System.err.println("[Leader] Depolama dizini oluşturulamadı: " + e.getMessage());
        }
    }

    private void loadToleranceConfig() {
        try {
            Path configFile = Paths.get("tolerance.conf");
            if (Files.exists(configFile)) {
                List<String> lines = Files.readAllLines(configFile);
                for (String line : lines) {
                    if (line.trim().startsWith("tolerance=")) {
                        toleranceLevel = Integer.parseInt(line.split("=")[1].trim());
                        System.out.println("[Leader] Hata tolerans seviyesi: " + toleranceLevel);
                        return;
                    }
                }
            } else {
                System.out.println("[Leader] tolerance.conf bulunamadı, varsayılan değer (2) kullanılıyor.");
                toleranceLevel = 2;
            }
        } catch (IOException e) {
            System.err.println("[Leader] tolerance.conf okunamadı, varsayılan değer (2) kullanılıyor");
            toleranceLevel = 2;
        }
    }

    private void loadMessageMapping() {
        try {
            Path mappingFile = Paths.get(storageDir, "message_mapping.txt");
            if (Files.exists(mappingFile)) {
                List<String> lines = Files.readAllLines(mappingFile);
                for (String line : lines) {
                    String[] parts = line.split(":");
                    if (parts.length < 2) continue;
                    int messageId = Integer.parseInt(parts[0]);
                    List<Integer> memberIds = Arrays.stream(parts[1].split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    messageToMembersMap.put(messageId, memberIds);
                }
                System.out.println("[Leader] " + messageToMembersMap.size() + " mesaj eşlemesi yüklendi");
            }
        } catch (IOException e) {
            System.err.println("[Leader] Mesaj eşlemesi yüklenemedi: " + e.getMessage());
        }
    }

    private void saveMessageMapping(int messageId, List<Integer> memberIds) {
        try {
            Path mappingFile = Paths.get(storageDir, "message_mapping.txt");
            String line = messageId + ":" + memberIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")) + "\n";
            Files.write(mappingFile, line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[Leader] Mesaj eşlemesi kaydedilemedi: " + e.getMessage());
        }
    }

    public void start() throws IOException {
        grpcServer = ServerBuilder.forPort(port)
                .addService(new LeaderServiceImpl())
                .build()
                .start();
        
        System.out.println("[Leader] gRPC sunucusu port " + port + " üzerinde başlatıldı");

        startClientServer();

        scheduler.scheduleAtFixedRate(this::printStats, 15, 15, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkMemberHealth, 5, 5, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("[Leader] Kapatılıyor...");
            Leader.this.stop();
        }));
    }

    private void startClientServer() {
        new Thread(() -> {
            try {
                clientServer = new ServerSocket(clientPort);
                System.out.println("[Leader] İstemci sunucusu port " + clientPort + " üzerinde başlatıldı");
                
                while (!clientServer.isClosed()) {
                    Socket clientSocket = clientServer.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                if (!clientServer.isClosed()) {
                    System.err.println("[Leader] İstemci sunucusu hatası: " + e.getMessage());
                }
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            String request = in.readLine();
            if (request == null) return;

            System.out.println("[Leader] İstemci isteği: " + request);
            String[] parts = request.split(" ", 3);
            String command = parts[0];

            if (command.equals("SET") && parts.length >= 3) {
                int messageId = Integer.parseInt(parts[1]);
                String message = parts[2];
                boolean success = handleSetMessage(messageId, message);
                out.println(success ? "OK" : "ERROR");
            } else if (command.equals("GET") && parts.length >= 2) {
                int messageId = Integer.parseInt(parts[1]);
                String message = handleGetMessage(messageId);
                out.println(message != null ? message : "ERROR: Mesaj bulunamadı");
            } else {
                out.println("ERROR: Geçersiz komut");
            }
        } catch (Exception e) {
            System.err.println("[Leader] İstemci işleme hatası: " + e.getMessage());
        }
    }

    private boolean handleSetMessage(int messageId, String message) {
        List<Integer> aliveMemberIds = members.values().stream()
                .filter(m -> m.isAlive)
                .map(m -> m.memberId)
                .collect(Collectors.toList());

        if (aliveMemberIds.size() < toleranceLevel) {
            System.err.println("[Leader] Yeterli canlı üye yok! Gerekli: " + toleranceLevel + ", Mevcut: " + aliveMemberIds.size());
            return false;
        }

        List<Integer> selectedMembers = selectMembersForMessage(aliveMemberIds);
        
        int successCount = 0;
        for (int memberId : selectedMembers) {
            MemberInfo member = members.get(memberId);
            try {
                StoreMessageRequest request = StoreMessageRequest.newBuilder()
                        .setMessageId(messageId)
                        .setMessageContent(message)
                        .setMemberId(memberId)
                        .build();
                
                StoreMessageResponse response = member.stub.storeMessage(request);
                if (response.getSuccess()) {
                    successCount++;
                    System.out.println("[Leader] Mesaj Member-" + memberId + " tarafından kaydedildi");
                }
            } catch (Exception e) {
                System.err.println("[Leader] Member-" + memberId + " mesajı kaydedemedi: " + e.getMessage());
                member.isAlive = false;
            }
        }

        if (successCount >= toleranceLevel) {
            messageToMembersMap.put(messageId, selectedMembers);
            saveMessageMapping(messageId, selectedMembers);
            return true;
        }

        return false;
    }

    private List<Integer> selectMembersForMessage(List<Integer> aliveMemberIds) {
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < toleranceLevel && i < aliveMemberIds.size(); i++) {
            int index = (nextMemberIndex + i) % aliveMemberIds.size();
            selected.add(aliveMemberIds.get(index));
        }
        nextMemberIndex = (nextMemberIndex + toleranceLevel) % Math.max(1, aliveMemberIds.size());
        return selected;
    }

    private String handleGetMessage(int messageId) {
        List<Integer> memberIds = messageToMembersMap.get(messageId);
        if (memberIds == null || memberIds.isEmpty()) {
            return null;
        }

        for (int memberId : memberIds) {
            MemberInfo member = members.get(memberId);
            if (member == null || !member.isAlive) continue;

            try {
                RetrieveMessageRequest request = RetrieveMessageRequest.newBuilder()
                        .setMessageId(messageId)
                        .build();
                
                RetrieveMessageResponse response = member.stub.retrieveMessage(request);
                if (response.getSuccess()) {
                    System.out.println("[Leader] Mesaj Member-" + memberId + " tarafından alındı");
                    return response.getMessageContent();
                }
            } catch (Exception e) {
                System.err.println("[Leader] Member-" + memberId + " mesajı alamadı: " + e.getMessage());
                member.isAlive = false;
            }
        }

        return null;
    }

    private void checkMemberHealth() {
        for (MemberInfo member : members.values()) {
            try {
                HeartbeatRequest request = HeartbeatRequest.newBuilder()
                        .setMemberId(member.memberId)
                        .build();
                HeartbeatResponse response = member.stub.heartbeat(request);
                member.isAlive = response.getAlive();
            } catch (Exception e) {
                if (member.isAlive) {
                    System.err.println("[Leader] Member-" + member.memberId + " artık yanıt vermiyor (Heartbeat Failed)");
                }
                member.isAlive = false;
            }
        }
    }

    private void printStats() {
        System.out.println("\n========== LİDER İSTATİSTİKLERİ ==========");
        System.out.println("Toplam kayıtlı mesaj: " + messageToMembersMap.size());
        System.out.println("Kayıtlı üye sayısı: " + members.size());
        
        for (MemberInfo member : members.values()) {
            try {
                if (member.isAlive) {
                    StatsRequest request = StatsRequest.newBuilder()
                            .setMemberId(member.memberId)
                            .build();
                    StatsResponse response = member.stub.getMemberStats(request);
                    System.out.println("  Member-" + member.memberId + ": " + 
                            response.getMessageCount() + " mesaj (Durum: Canlı)");
                } else {
                    System.out.println("  Member-" + member.memberId + ": (Durum: ÖLÜ - Yanıt Yok)");
                }
            } catch (Exception e) {
                System.out.println("  Member-" + member.memberId + ": İstatistik alınamadı (Durum: Ölü)");
                member.isAlive = false;
            }
        }
        System.out.println("==========================================\n");
    }

    private class LeaderServiceImpl extends FamilyServiceGrpc.FamilyServiceImplBase {
        @Override
        public void registerMember(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
            try {
                int memberId = request.getMemberId();
                String host = request.getHost();
                int port = request.getPort();

                System.out.println("[Leader] Kayıt isteği alındı -> ID: " + memberId + ", Host: " + host + ", Port: " + port);

                // MemberInfo oluşturulurken hata olursa diye try-catch dışına aldık
                MemberInfo member = new MemberInfo(memberId, host, port);
                members.put(memberId, member);
                
                System.out.println("[Leader] ✅ Yeni üye başarıyla havuza eklendi: Member-" + memberId);

                RegisterResponse response = RegisterResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Başarıyla kaydedildi")
                        .setToleranceLevel(toleranceLevel)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                // İŞTE BURASI: Hata olursa Lider konsoluna basacak!
                System.err.println("[Leader] ❌ Kayıt sırasında HATA OLUŞTU:");
                e.printStackTrace(); // Hatanın tüm detayını bas

                // Üyeye de hata olduğunu söyle
                responseObserver.onError(e);
            }
        }
    }

    public void stop() {
        if (grpcServer != null) {
            grpcServer.shutdown();
        }
        if (clientServer != null && !clientServer.isClosed()) {
            try {
                clientServer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (MemberInfo member : members.values()) {
            member.shutdown();
        }
        scheduler.shutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Kullanım: java Leader <grpc_port> <client_port>");
            System.err.println("Örnek: java Leader 50051 8080");
            System.exit(1);
        }

        int grpcPort = Integer.parseInt(args[0]);
        int clientPort = Integer.parseInt(args[1]);

        Leader leader = new Leader(grpcPort, clientPort);
        leader.start();
        leader.blockUntilShutdown();
    }
}