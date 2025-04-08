package com.example.clpmonitor.model;

public class ClpData {
    private int clpId;
    private Object value;

    public ClpData(int clpId, Object value) {
        this.clpId = clpId;
        this.value = value;
    }

    public int getClpId() {
        return clpId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
