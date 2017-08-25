package com.spotify.apollo.web.session;

import com.spotify.apollo.web.Session;

public interface SessionFactory<T extends Session> {

    public T createSession(String key);
}
