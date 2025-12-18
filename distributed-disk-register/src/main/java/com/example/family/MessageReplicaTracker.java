package com.example.family;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import family.NodeInfo;

public class MessageReplicaTracker {
    private final Map<Integer, List<NodeInfo>> messageToMembers = new ConcurrentHashMap<>();

    public void addReplica(int messageId, NodeInfo member) {
        messageToMembers.computeIfAbsent(messageId, k -> new ArrayList<>()).add(member);
    }

    public List<NodeInfo> getMembersForMessage(int messageId) {
        return messageToMembers.getOrDefault(messageId, new ArrayList<>());
    }

    public void removeDeadMember(NodeInfo deadMember) {
        for (List<NodeInfo> members : messageToMembers.values()) {
            members.removeIf(m -> m.getHost().equals(deadMember.getHost()) 
                              && m.getPort() == deadMember.getPort());
        }
    }

    public void printStats() {
        System.out.println("=== Message Replica Stats ===");
        System.out.println("Total messages tracked: " + messageToMembers.size());
        messageToMembers.forEach((id, members) -> {
            System.out.printf("Message %d -> %d replicas%n", id, members.size());
        });
    }
    public Map<Integer, List<NodeInfo>> getSnapshot() {
        // Orijinal map bozulmasın diye salt okunur veya kopya döndürüyoruz
        return Collections.unmodifiableMap(messageToMembers);
    }
}
