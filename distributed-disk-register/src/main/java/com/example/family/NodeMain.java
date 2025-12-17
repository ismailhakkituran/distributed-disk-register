package com.example.family;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.family.SetGetCommand.Command;
import com.example.family.SetGetCommand.CommandParser;
import com.example.family.SetGetCommand.DataStore;

import family.ChatMessage;
import family.Empty;
import family.FamilyServiceGrpc;
import family.FamilyView;
import family.NodeInfo;
import family.StorageServiceGrpc;
import family.StoredMessage;
import family.MessageId;
import family.StoreResult;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;

import com.example.family.SetGetCommand.SetCommand;
import com.example.family.SetGetCommand.GetCommand;

public class NodeMain {

    private static final int START_PORT = 5555;
    private static final int PRINT_INTERVAL_SECONDS = 10;
    // SET/GET verilerini tuttuğumuz Map
    private static final DataStore STORE = new DataStore();
    private static final MessageReplicaTracker REPLICA_TRACKER = new MessageReplicaTracker();

    public static void main(String[] args) throws Exception {
        ToleranceConfig.loadConfig();
        
        String host = "127.0.0.1";
        int port = findFreePort(START_PORT); // 5555 ve sonrası için boş olan ilk portu verir

        NodeInfo self = NodeInfo.newBuilder() // Üyenin kendisi
                .setHost(host)
                .setPort(port)
                .build();

        NodeRegistry registry = new NodeRegistry();
        FamilyServiceImpl service = new FamilyServiceImpl(registry, self);

        StorageServiceImpl storageService = new StorageServiceImpl(STORE);

        Server server = ServerBuilder
                .forPort(port)
                .addService(service)
                .addService(storageService)
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

    private static void handleClientTextConnection(Socket client,
            NodeRegistry registry,
            NodeInfo self) {
        System.out.println("New TCP client connected: " + client.getRemoteSocketAddress());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String text = line.trim();
                if (text.isEmpty()) {
                    continue;
                }

                // Kendi üstüne de yaz
                System.out.println(" Received from TCP: " + text);

                try {
                    // 1) Komutu parse et
                    Command cmd = CommandParser.parse(text);

                    String result;

                    if (cmd instanceof SetCommand setCmd) {
                        int messageId = setCmd.getKey();
                        String messageText = setCmd.getValue();
                        
                        // Memory'e yaz
                        STORE.set(messageId, messageText);

                        // Disk'e yaz
                        writeMessageToDisk(messageId, messageText);

                        // Distributed replication
                        result = replicateToMembers(registry, self, messageId, messageText);

                    } else if (cmd instanceof GetCommand getCmd) {
                        int messageId = getCmd.getKey();
                        
                        // Diskten oku
                        String value = readMessageFromDisk(messageId);

                        if (value == null) {
                            // Kendi diskinde yoksa, üyelerden almayı dene
                            value = retrieveFromMembers(messageId);
                        }
                        
                        if (value == null) {
                            result = "NOT_FOUND";
                        } else {
                            result = value;
                        }

                    } else {
                        result = "ERROR: Unknown command";
                    }
                    // Client'a cevabı yolluyoruz
                    PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                    writer.println(result);

                    long ts = System.currentTimeMillis();
                    ChatMessage msg = ChatMessage.newBuilder()
                            .setText(text)
                            .setFromHost(self.getHost())
                            .setFromPort(self.getPort())
                            .setTimestamp(ts)
                            .build();

                    // Tüm family üyelerine broadcast et
                    broadcastToFamily(registry, self, msg);

                } catch (IllegalArgumentException e) {
                    // Hatalı komut → ERROR dön
                    System.out.println("ERROR: " + e.getMessage());
                }

            }

        } catch (IOException e) {
            System.err.println("TCP client handler error: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
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

                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                stub.receiveChat(msg);

                System.out.printf("Broadcasted message to %s:%d%n", n.getHost(), n.getPort());

            } catch (Exception e) {
                System.err.printf("Failed to send to %s:%d (%s)%n",
                        n.getHost(), n.getPort(), e.getMessage());
            } finally {
                if (channel != null) {
                    channel.shutdownNow();
                }
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

                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                // Karşılıklı tanışma
                FamilyView view = stub.join(self);
                registry.addAll(view.getMembersList());

                System.out.printf("Joined through %s:%d, family size now: %d%n",
                        host, port, registry.snapshot().size());

            } catch (Exception ignored) {
            } finally {
                if (channel != null) {
                    channel.shutdownNow();
                }
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

                    FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

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

    private static final File MESSAGE_DIR = new File("messages");

    static {
        if (!MESSAGE_DIR.exists()) {
            MESSAGE_DIR.mkdirs();
        }
    }

    private static void writeMessageToDisk(int id, String msg) { // String id -> int id
        File file = new File(MESSAGE_DIR, id + ".msg");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readMessageFromDisk(int id) { // String id -> int id
        File file = new File(MESSAGE_DIR, id + ".msg");
        if (!file.exists()) {
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String replicateToMembers(NodeRegistry registry, NodeInfo self, int messageId, String messageText) {
        int tolerance = ToleranceConfig.getTolerance();
        List<NodeInfo> allMembers = registry.snapshot();
        
        List<NodeInfo> eligibleMembers = new ArrayList<>();
        for (NodeInfo member : allMembers) {
            if (!(member.getHost().equals(self.getHost()) && member.getPort() == self.getPort())) {
                eligibleMembers.add(member);
            }
        }

        if (eligibleMembers.isEmpty()) {
            System.out.println("No members available for replication, only leader exists");
            return "OK";
        }

        int replicasNeeded = Math.min(tolerance, eligibleMembers.size());
        List<NodeInfo> selectedMembers = eligibleMembers.subList(0, replicasNeeded);

        int successCount = 0;
        for (NodeInfo member : selectedMembers) {
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder
                        .forAddress(member.getHost(), member.getPort())
                        .usePlaintext()
                        .build();

                StorageServiceGrpc.StorageServiceBlockingStub stub = 
                        StorageServiceGrpc.newBlockingStub(channel);

                StoredMessage msg = StoredMessage.newBuilder()
                        .setId(messageId)
                        .setText(messageText)
                        .build();

                StoreResult result = stub.store(msg);

                if (result.getSuccess()) {
                    REPLICA_TRACKER.addReplica(messageId, member);
                    successCount++;
                    System.out.printf("Replicated message %d to %s:%d%n", 
                            messageId, member.getHost(), member.getPort());
                }

            } catch (Exception e) {
                System.err.printf("Failed to replicate to %s:%d - %s%n",
                        member.getHost(), member.getPort(), e.getMessage());
            } finally {
                if (channel != null) {
                    channel.shutdownNow();
                }
            }
        }

        if (successCount == replicasNeeded) {
            return "OK";
        } else {
            return "ERROR: Failed to replicate to all required members (" + successCount + "/" + replicasNeeded + ")";
        }
    }

    private static String retrieveFromMembers(int messageId) {
        List<NodeInfo> members = REPLICA_TRACKER.getMembersForMessage(messageId);
        
        if (members.isEmpty()) {
            System.out.println("No replica information found for message " + messageId);
            return null;
        }

        for (NodeInfo member : members) {
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder
                        .forAddress(member.getHost(), member.getPort())
                        .usePlaintext()
                        .build();

                StorageServiceGrpc.StorageServiceBlockingStub stub = 
                        StorageServiceGrpc.newBlockingStub(channel);

                MessageId msgId = MessageId.newBuilder()
                        .setId(messageId)
                        .build();

                StoredMessage response = stub.retrieve(msgId);

                if (response != null && !response.getText().isEmpty()) {
                    System.out.printf("Retrieved message %d from %s:%d%n", 
                            messageId, member.getHost(), member.getPort());
                    return response.getText();
                }

            } catch (Exception e) {
                System.err.printf("Failed to retrieve from %s:%d - %s%n",
                        member.getHost(), member.getPort(), e.getMessage());
            } finally {
                if (channel != null) {
                    channel.shutdownNow();
                }
            }
        }

        return null;
    }
}
