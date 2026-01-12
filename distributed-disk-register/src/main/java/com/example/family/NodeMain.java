package com.example.family;

import family.Empty;
import family.FamilyServiceGrpc;
import family.FamilyView;
import family.NodeInfo;
import family.ChatMessage;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;


import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;


public class NodeMain {
    private static final java.util.Map<Integer, java.util.List<family.NodeInfo>> messageLocationMap = new java.util.concurrent.ConcurrentHashMap<>();
    private static final DiskManager diskManager = new DiskManager();
    private static final int START_PORT = 5555;
    private static final int PRINT_INTERVAL_SECONDS = 10;
    private static int TOLERANCE = 1;
    private static boolean isLeader = false;
    private static ServerSocket leaderSocket = null;

    public static void main(String[] args) throws Exception {
        loadToleranceConfig();
        String host = "127.0.0.1";
        int port = findFreePort(START_PORT);

        NodeInfo self = NodeInfo.newBuilder()
                .setHost(host)
                .setPort(port)
                .build();

        NodeRegistry registry = new NodeRegistry();
        FamilyServiceImpl service = new FamilyServiceImpl(registry, self);

        Server server = ServerBuilder
                .forPort(port)
                .addService(service)
                .addService(new StorageServiceImpl(diskManager))
                .build()
                .start();

                System.out.printf("Node started on %s:%d%n", host, port);
                discoverExistingNodes(host, port, registry, self);
                checkLeadership(registry, self);
                startFamilyPrinter(registry, self);
                startHealthChecker(registry, self);

                server.awaitTermination();




    }
    private static void startLeaderTextListener(NodeRegistry registry, NodeInfo self) {
        new Thread(() -> {
            try {
                // Eğer önceki bir soket açıksa kapatmaya çalış (Temizlik)
                if (leaderSocket != null && !leaderSocket.isClosed()) {
                    leaderSocket.close();
                }

                leaderSocket = new ServerSocket();
                leaderSocket.setReuseAddress(true); // Portu hemen serbest bırakır
                leaderSocket.bind(new java.net.InetSocketAddress(6666));
                System.out.printf("📢 Lider Modu Aktif: TCP %s:%d üzerinde dinleniyor...%n", self.getHost(), 6666);

                while (isLeader && !leaderSocket.isClosed()) {
                    try {
                        Socket client = leaderSocket.accept();
                        new Thread(() -> handleClientTextConnection(client, registry, self)).start();
                    } catch (IOException e) {
                        // Liderlik düştüyse veya soket kapandıysa döngüden çık
                        if (isLeader) System.err.println("Lider dinleme hatası: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
                System.err.println("⚠️ Lider portu (6666) açılamadı! Belki başka bir lider var? Hata: " + e.getMessage());
                // Port hatası aldıysak muhtemelen lider değilizdir veya port doludur.
                // isLeader = false; // Opsiyonel: Duruma göre liderliği bırakabilir.
            }
        }, "LeaderTextListener").start();
    }

    private static void handleClientTextConnection(Socket client, NodeRegistry registry, NodeInfo self) {
        System.out.println("Yeni TCP istemcisi bağlandı: " + client.getRemoteSocketAddress());
        // reader okumak için, writer ise istemciye (terminale) cevap yazmak için
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter writer = new PrintWriter(client.getOutputStream(), true)) {

            String line;
            while ((line = reader.readLine()) != null) {
                String text = line.trim();
                if (text.isEmpty()) continue;

                System.out.println("📝 Gelen Komut: " + text);

                // 1. Komutu ayrıştır (Parser'ı kullanıyoruz)
                Command cmd = CommandParser.parse(text);

                if (cmd instanceof SetCommand sc) {

                    boolean localSuccess = diskManager.saveMessage(sc.id(), sc.message());

                    if (localSuccess) {
                        java.util.List<family.NodeInfo> savedNodes = new java.util.ArrayList<>();
                        savedNodes.add(self);

                        java.util.List<family.NodeInfo> members = registry.snapshot();
                        java.util.List<family.NodeInfo> others = members.stream()
                                .filter(n -> !(n.getHost().equals(self.getHost()) && n.getPort() == self.getPort()))
                                .collect(java.util.stream.Collectors.toList());



                        int distributedCount = 0;
                        if (!others.isEmpty()) {
                            int startIndex = sc.id() % others.size();

                            for (int i = 0; i < others.size() && distributedCount < TOLERANCE; i++) {
                                int targetIndex = (startIndex + i) % others.size();
                                family.NodeInfo target = others.get(targetIndex);

                                if (sendStoreRequest(target, sc)) {
                                    savedNodes.add(target);
                                    distributedCount++;
                                }
                            }
                        }


                        messageLocationMap.put(sc.id(), savedNodes);

                        if (distributedCount >= TOLERANCE || (others.size() < TOLERANCE && distributedCount == others.size())) {
                            writer.println("OK");
                            System.out.println("✅ ID=" + sc.id() + " başarıyla " + savedNodes.size() + " node'a dağıtıldı.");
                        } else {
                            writer.println("ERROR: Yeterli kopyalama yapılamadı.");
                        }
                    } else {
                        writer.println("ERROR: Lider diske yazamadı.");
                    }
                }
                else if (cmd instanceof GetCommand gc) {
                    // 1. Önce kendi diskine bak
                    String content = diskManager.loadMessage(gc.id());

                    if (content != null) {
                        writer.println(content);
                    } else {
                        // 2. Kendi diskinde yoksa, haritadan kimde olduğuna bak
                        java.util.List<family.NodeInfo> owners = messageLocationMap.getOrDefault(gc.id(), java.util.Collections.emptyList());

                        String foundContent = null;
                        for (family.NodeInfo owner : owners) {
                            if (owner.getPort() == self.getPort()) continue;

                            System.out.println("🔍 Mesaj " + owner.getPort() + " portundan isteniyor...");
                            foundContent = fetchFromMember(owner, gc.id());

                            // Eğer üye crash olmuşsa veya ulaşılmazsa foundContent null döner
                            if (foundContent != null && !foundContent.equals("NOT_FOUND")) {
                                System.out.println("✅ Mesaj yedek üyeden başarıyla alındı.");
                                break;
                            } else {
                                System.out.println("⚠️ Üye cevap vermedi veya mesajı bulamadı, sıradaki yedeğe geçiliyor...");
                            }
                        }

                        if (foundContent != null) {
                            writer.println(foundContent);
                        } else {
                            writer.println("NOT_FOUND");
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("TCP istemci hatası: " + e.getMessage());
        }
            try { client.close(); } catch (IOException ignored) {}
    }

    private static void broadcastToFamily(NodeRegistry registry,
                                      NodeInfo self,
                                      ChatMessage msg) {

    List<NodeInfo> members = registry.snapshot();

    for (NodeInfo n : members) {
        // Kendimize tekrar gönderme
        if (n.getHost().equals(self.getHost()) && n.getPort() == self.getPort()) {
            continue;
        }

        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder
                    .forAddress(n.getHost(), n.getPort())
                    .usePlaintext()
                    .build();

            FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                    FamilyServiceGrpc.newBlockingStub(channel);

            stub.receiveChat(msg);

            System.out.printf("Broadcasted message to %s:%d%n", n.getHost(), n.getPort());

        } catch (Exception e) {
            System.err.printf("Failed to send to %s:%d (%s)%n",
                    n.getHost(), n.getPort(), e.getMessage());
        } finally {
            if (channel != null) channel.shutdownNow();
        }
    }
}


    private static int findFreePort(int startPort) {
        int port = startPort;
        while (true) {
            try (ServerSocket ignored = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                port++;
            }
        }
    }

    private static void discoverExistingNodes(String host,
                                              int selfPort,
                                              NodeRegistry registry,
                                              NodeInfo self) {

        for (int port = START_PORT; port < selfPort; port++) {
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder
                        .forAddress(host, port)
                        .usePlaintext()
                        .build();

                FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                        FamilyServiceGrpc.newBlockingStub(channel);

                FamilyView view = stub.join(self);
                registry.addAll(view.getMembersList());

                System.out.printf("Joined through %s:%d, family size now: %d%n",
                        host, port, registry.snapshot().size());

            } catch (Exception e) {
                System.out.printf("⚠️ UYARI: %d portuna bağlanılamadı. Sebep: %s%n", port, e.getMessage());
            } finally {
                if (channel != null) channel.shutdownNow();
            }
        }
    }

    private static void startFamilyPrinter(NodeRegistry registry, NodeInfo self) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            List<NodeInfo> members = registry.snapshot();
            System.out.println("======================================");
            System.out.printf("Family at %s:%d (me)%n", self.getHost(), self.getPort());
            System.out.println("Time: " + LocalDateTime.now());


            if (isLeader) {
                System.out.println("Sistem Mesaj Dağılım İstatistikleri:");

                java.util.Map<String, Long> counts = new java.util.HashMap<>();
                messageLocationMap.values().forEach(nodes -> {
                    nodes.forEach(n -> {
                        String key = n.getHost() + ":" + n.getPort();
                        counts.put(key, counts.getOrDefault(key, 0L) + 1);
                    });
                });
                counts.forEach((node, count) -> System.out.printf("   > %s : %d mesaj saklıyor%n", node, count));
            }

            System.out.println("Members:");
            for (NodeInfo n : members) {
                boolean isMe = n.getHost().equals(self.getHost()) && n.getPort() == self.getPort();
                System.out.printf(" - %s:%d%s%n", n.getHost(), n.getPort(), isMe ? " (me)" : "");
            }
            System.out.println("======================================");
        }, 3, PRINT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private static void startHealthChecker(NodeRegistry registry, NodeInfo self) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            List<NodeInfo> members = registry.snapshot();

            for (NodeInfo n : members) {
                // Kendimizi kontrol etmeyelim
                if (n.getHost().equals(self.getHost()) && n.getPort() == self.getPort()) {
                    continue;
                }

                ManagedChannel channel = null;
                try {
                    channel = ManagedChannelBuilder
                            .forAddress(n.getHost(), n.getPort())
                            .usePlaintext()
                            .build();

                    FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                            FamilyServiceGrpc.newBlockingStub(channel);

                    // Ping gibi kullanıyoruz: cevap bizi ilgilendirmiyor,
                    // sadece RPC'nin hata fırlatmaması önemli.
                    stub.getFamily(Empty.newBuilder().build());

                } catch (Exception e) {
                    // Bağlantı yok / node ölmüş → listeden çıkar
                    System.out.printf("Node %s:%d unreachable, removing from family%n",
                            n.getHost(), n.getPort());
                    registry.remove(n);
                    checkLeadership(registry, self);
                } finally {
                    if (channel != null) {
                        channel.shutdownNow();
                    }
                }
            }

        }, 5, 10, TimeUnit.SECONDS); // 5 sn sonra başla, 10 sn'de bir kontrol et
    }
        private static void loadToleranceConfig() {
            java.io.File file = new java.io.File("tolerance.conf");
            if (!file.exists()) {
                System.out.println("⚠️ tolerance.conf bulunamadı, varsayılan TOLERANCE=1 kullanılıyor.");
                return;
            }
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line = br.readLine();
                if (line != null && line.startsWith("TOLERANCE=")) {
                    TOLERANCE = Integer.parseInt(line.split("=")[1].trim());
                }
            } catch (Exception e) {
                System.err.println("Konfig okuma hatası: " + e.getMessage());
            }
        }
        private static boolean sendStoreRequest(family.NodeInfo target, SetCommand cmd) {
            io.grpc.ManagedChannel channel = null;
            try {
                channel = io.grpc.ManagedChannelBuilder
                        .forAddress(target.getHost(), target.getPort())
                        .usePlaintext()
                        .build();

                family.StorageServiceGrpc.StorageServiceBlockingStub stub =
                        family.StorageServiceGrpc.newBlockingStub(channel);

                family.StoredMessage msg = family.StoredMessage.newBuilder()
                        .setId(cmd.id())
                        .setText(cmd.message())
                        .build();

                family.StoreResult result = stub.store(msg);
                return result.getSuccess();
            } catch (Exception e) {
                System.err.printf("-> Kopya hatası %s:%d : %s%n", target.getHost(), target.getPort(), e.getMessage());
                return false;
            } finally {
                if (channel != null) channel.shutdownNow();
            }
        }
    private static String fetchFromMember(family.NodeInfo target, int id) {
        io.grpc.ManagedChannel channel = io.grpc.ManagedChannelBuilder.forAddress(target.getHost(), target.getPort()).usePlaintext().build();
        try {
            family.StorageServiceGrpc.StorageServiceBlockingStub stub = family.StorageServiceGrpc.newBlockingStub(channel);
            family.MessageId request = family.MessageId.newBuilder().setId(id).build();
            family.StoredMessage response = stub.retrieve(request);
            return response.getText();
        } catch (Exception e) { return null; }
        finally { channel.shutdownNow(); }
    }
    private static synchronized void checkLeadership(NodeRegistry registry, NodeInfo self) {
        List<NodeInfo> members = registry.snapshot();

        if (members.isEmpty()) {
            if (!isLeader) {
                promoteToLeader(registry, self);
            }
            return;
        }

        int minPort = Integer.MAX_VALUE;
        for (NodeInfo n : members) {
            if (n.getPort() < minPort) {
                minPort = n.getPort();
            }
        }
        if (self.getPort() < minPort) {
            minPort = self.getPort();
        }

        if (self.getPort() == minPort) {
            if (!isLeader) {
                System.out.println("👑 TEBRİKLER! Bu node artık YENİ LİDER ilan edildi.");
                promoteToLeader(registry, self);
            }
        }else {
            System.out.println("🛡️ Lider değilim, yedek olarak bekliyorum.");
        }
    }

    private static void promoteToLeader(NodeRegistry registry, NodeInfo self) {
        isLeader = true;
        startLeaderTextListener(registry, self);
    }

}
