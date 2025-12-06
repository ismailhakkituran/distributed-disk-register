package com.example.family.SetGetCommand;

public class CommandParser {

    public static Command parse(String line) {
        String[] parts = line.split(" ", 3);

        if (parts.length < 2)
            throw new IllegalArgumentException("Invalid command");

        String cmd = parts[0].toUpperCase();

        switch (cmd) {
            case "SET":
                if (parts.length < 3)
                    throw new IllegalArgumentException("SET requires id and value");
                return new SetCommand(parts[1], parts[2]);

            case "GET":
                return new GetCommand(parts[1]);

            default:
                throw new IllegalArgumentException("Unknown command: " + cmd);
        }
    }
}
