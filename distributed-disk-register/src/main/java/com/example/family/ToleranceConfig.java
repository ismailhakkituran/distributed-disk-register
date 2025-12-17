package com.example.family;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ToleranceConfig {
    private static final String CONFIG_FILE = "tolerance.conf";
    private static int tolerance = 2;

    public static int getTolerance() {
        return tolerance;
    }

    public static void loadConfig() {
        try (BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("TOLERANCE=")) {
                    String value = line.substring("TOLERANCE=".length()).trim();
                    tolerance = Integer.parseInt(value);
                    System.out.println("Loaded tolerance configuration: " + tolerance);
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read tolerance.conf, using default: " + tolerance);
        } catch (NumberFormatException e) {
            System.err.println("Invalid tolerance value in config, using default: " + tolerance);
        }
    }
}
