package com.example.family;

public record SetCommand(int id, String message) implements Command{
}
