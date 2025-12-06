package com.example.family.SetGetCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    
    private final Map<String, String> map = new ConcurrentHashMap<>();

    public String set(String key, String value) {
        map.put(key, value);
        return "OK";
    }

    public String get(String key) {
        return map.getOrDefault(key, "NOT_FOUND");
    }

}
