package com.callcenter.ftcjsc.services;

public class MessageEvent {
    public static String globalMessage = "";
    public final String message;
    public MessageEvent(String message) {
        this.message = message;
        globalMessage = message;
    }
}
