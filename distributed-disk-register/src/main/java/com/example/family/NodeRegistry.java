package com.example.family;

import family.NodeInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {

    private final Set<NodeInfo> nodes = ConcurrentHashMap.newKeySet();

    public void add(NodeInfo node) {
        nodes.add(node);
    }

    public void addAll(Collection<NodeInfo> others) {
        nodes.addAll(others);
    }

    public List<NodeInfo> snapshot() {
        return List.copyOf(nodes);
    }

    public void remove(NodeInfo node) {
        nodes.remove(node);
    }
}
