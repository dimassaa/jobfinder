package com.jobapp.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {

    private String type;
    private String command;
    private Object data;

    @JsonCreator
    public Message(
            @JsonProperty("type") String type,
            @JsonProperty("command") String command,
            @JsonProperty("data") Object data) {
        this.type = type;
        this.command = command;
        this.data = data;
    }

    public Message() {
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("command")
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @JsonProperty("data")
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", command='" + command + '\'' +
                ", data=" + data +
                '}';
    }
}