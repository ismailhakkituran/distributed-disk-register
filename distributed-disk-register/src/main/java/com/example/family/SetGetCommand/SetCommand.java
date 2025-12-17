package com.example.family.SetGetCommand;

import family.StoredMessage;

public class SetCommand implements Command {

    private final StoredMessage message;

    public SetCommand(String key, String value) {
        this.message = StoredMessage.newBuilder()
                                    .setId(Integer.parseInt(key))
                                    .setText(value)
                                    .build();
    }

    // --- EKLENEN GETTER METODLARI ---
    public int getKey() {
        return message.getId();
    }

    public String getValue() {
        return message.getText();
    }
    // --------------------------------

    @Override
    public String execute(DataStore store) {
        return store.set(message.getId(), message.getText());
    }
}
