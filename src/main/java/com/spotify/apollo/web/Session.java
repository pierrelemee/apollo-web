package com.spotify.apollo.web;

public abstract class Session {

    protected final String key;

    public Session(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
