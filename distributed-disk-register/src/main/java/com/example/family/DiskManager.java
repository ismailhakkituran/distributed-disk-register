package com.example.family;

import java.io.*;

public class DiskManager {
    private static final String STORAGE_DIR = "messages";

    public DiskManager() {
        // Klasör yoksa oluştur
        File directory = new File(STORAGE_DIR);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    // Mesajı diske kaydet (SET)
    public boolean saveMessage(int id, String message) {
        try {
            File file = new File(STORAGE_DIR, id + ".msg");
            // Append false -> üzerine yazar (update)
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                writer.write(message);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Disk yazma hatası: " + e.getMessage());
            return false;
        }
    }

    // Mesajı diskten oku (GET)
    public String loadMessage(int id) {
        File file = new File(STORAGE_DIR, id + ".msg");
        if (!file.exists()) {
            return null; // Dosya yoksa null dön
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } catch (IOException e) {
            System.err.println("Disk okuma hatası: " + e.getMessage());
            return null;
        }
    }
}