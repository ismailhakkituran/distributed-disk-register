package com.example.family.SetGetCommand;

import family.StoredMessage;

public class GetCommand implements Command {

    private final StoredMessage message;

    public GetCommand(String key) {
        this.message = StoredMessage.newBuilder()
                                    .setId(Integer.parseInt(key))
                                    .build();
    }

    // --- EKLENEN GETTER METODU ---
    public int getKey() {
        return message.getId();
    }
    // -----------------------------

    @Override
    public String execute(DataStore store) {
        return store.get(message.getId());
    }
}
