package com.example.family.SetGetCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {

    private final Map<Integer, String> map = new ConcurrentHashMap<>(); // <String, String> -> <Integer, String>

    public String set(int key, String value) { // String key -> int key
        map.put(key, value);
        return "OK";
    }

    public String get(int key) { // String key -> int key
        return map.getOrDefault(key, "NOT_FOUND");
    }

}
