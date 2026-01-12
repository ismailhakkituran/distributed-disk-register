package com.example.family;

public class CommandParser {
    public static Command parse(String line) {
        try {
            String[] parts = line.split(" ", 3);
            String action = parts[0].toUpperCase();

            if (action.equals("SET")) {
                return new SetCommand(Integer.parseInt(parts[1]), parts[2]);
            } else if (action.equals("GET")) {
                return new GetCommand(Integer.parseInt(parts[1]));
            }
        } catch (Exception e) {
            System.out.println("Gecersiz komut formati: " + line);
        }
        return null;
    }
}
