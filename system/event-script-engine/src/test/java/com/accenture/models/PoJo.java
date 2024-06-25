package com.accenture.models;

import java.util.Date;

public class PoJo {

    public String user;
    public Date date;
    public int sequence;

    public String key1;
    public String key2;

    public PoJo(String user, int sequence) {
        this.user = user;
        this.sequence = sequence;
        date = new Date();
    }
}
