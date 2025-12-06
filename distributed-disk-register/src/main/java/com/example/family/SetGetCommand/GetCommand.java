package com.example.family.SetGetCommand;

public class GetCommand implements Command {

    private final String key;

    public GetCommand(String key) {
        this.key = key;
    }

// --- EKLENEN GETTER METODU ---
    public String getKey() {
        return key;
    }
    // -----------------------------

    @Override
    public String execute(DataStore store) {
        return store.get(key);
    }
}
