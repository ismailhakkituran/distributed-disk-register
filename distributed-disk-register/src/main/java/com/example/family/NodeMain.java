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


import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class NodeMain {
    private static final java.util.Map<Integer, String> messageMap = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int START_PORT = 5555;
    private static final int PRINT_INTERVAL_SECONDS = 10;

    public static void main(String[] args) throws Exception {
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
                .build()
                .start();

                System.out.printf("Node started on %s:%d%n", host, port);

                // Eğer bu ilk node ise (port 5555), TCP 6666'da text dinlesin
                if (port == START_PORT) {
                    startLeaderTextListener(registry, self);
                }

                discoverExistingNodes(host, port, registry, self);
                startFamilyPrinter(registry, self);
                startHealthChecker(registry, self);

                server.awaitTermination();




    }

    private static void startLeaderTextListener(NodeRegistry registry, NodeInfo self) {
    // Sadece lider (5555 portlu node) bu methodu çağırmalı
    new Thread(() -> {
        try (ServerSocket serverSocket = new ServerSocket(6666)) {
            System.out.printf("Leader listening for text on TCP %s:%d%n",
                    self.getHost(), 6666);

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClientTextConnection(client, registry, self)).start();
            }

        } catch (IOException e) {
            System.err.println("Error in leader text listener: " + e.getMessage());
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
                    // SET ise belleğe (map) yaz ve OK döndür
                    messageMap.put(sc.id(), sc.message());
                    writer.println("OK");
                    System.out.println("Sistem: ID=" + sc.id() + " kaydedildi.");
                }
                else if (cmd instanceof GetCommand gc) {
                    // GET ise map'ten bul, varsa yaz yoksa NOT_FOUND döndür
                    String result = messageMap.get(gc.id());
                    if (result != null) {
                        writer.println(result);
                    } else {
                        writer.println("NOT_FOUND");
                    }
                }
                else {
                    writer.println("ERROR: Geçersiz Format (SET <id> <msg> veya GET <id> kullanın)");
                }
            }

        } catch (IOException e) {
            System.err.println("TCP istemci hatası: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
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

            } catch (Exception ignored) {
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
            System.out.println("Members:");

            for (NodeInfo n : members) {
                boolean isMe = n.getHost().equals(self.getHost()) && n.getPort() == self.getPort();
                System.out.printf(" - %s:%d%s%n",
                        n.getHost(),
                        n.getPort(),
                        isMe ? " (me)" : "");
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
            } finally {
                if (channel != null) {
                    channel.shutdownNow();
                }
            }
        }

    }, 5, 10, TimeUnit.SECONDS); // 5 sn sonra başla, 10 sn'de bir kontrol et
}

}
