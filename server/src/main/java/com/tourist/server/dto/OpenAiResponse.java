package com.tourist.server.dto;

import java.util.List;

public record OpenAiResponse(
        List<Choice> choices
) {
    public record Choice(Message message) {}
    public record Message(String role, String content) {}

    public String content() {
        if (choices == null || choices.isEmpty() || choices.get(0).message() == null) {
            return null;
        }
        return choices.get(0).message().content();
    }
}
