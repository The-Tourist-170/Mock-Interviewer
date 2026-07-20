package com.tourist.server.dto;

import java.util.List;

public record GeminiRequest(List<Content> contents) {
    public static record Content(List<Part> parts) {}
    public static record Part(String text) {}
}