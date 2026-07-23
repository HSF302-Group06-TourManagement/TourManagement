package com.se190588.mvc.dto;

import java.util.List;

public class AiChatDto {
    private String message;
    private String reply;
    private List<String> suggestedQuestions;

    public AiChatDto() {
    }

    public AiChatDto(String reply) {
        this.reply = reply;
    }

    public AiChatDto(String reply, List<String> suggestedQuestions) {
        this.reply = reply;
        this.suggestedQuestions = suggestedQuestions;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<String> getSuggestedQuestions() {
        return suggestedQuestions;
    }

    public void setSuggestedQuestions(List<String> suggestedQuestions) {
        this.suggestedQuestions = suggestedQuestions;
    }
}
