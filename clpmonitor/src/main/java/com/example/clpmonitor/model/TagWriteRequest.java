package com.example.clpmonitor.model;

public class TagWriteRequest {
    private String ip;
    private int port;
    private int db;
    private String type;
    private int offset;
    private int bitNumber;
    private int size;
    private String value;

    // Getters e Setters
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public int getDb() {
        return db;
    }
    public void setDb(int db) {
        this.db = db;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public int getOffset() {
        return offset;
    }
    public void setOffset(int offset) {
        this.offset = offset;
    }
    public int getBitNumber() {
        return bitNumber;
    }
    public void setBitNumber(int bitNumber) {
        this.bitNumber = bitNumber;
    }
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
