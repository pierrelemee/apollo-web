package com.spotify.apollo.web.session;

import com.spotify.apollo.WebRequest;
import com.spotify.apollo.web.Session;

import java.util.Random;

public abstract class SessionManager<T extends Session> {

    protected SessionFactory<T> factory;
    protected Random random;

    public SessionManager(SessionFactory<T> factory) {
        this.factory = factory;
        this.random = new Random();
    }

    public T extract(WebRequest request) {
        if (request.hasCookie(this.getSessionCookieName())) {
            if (this.hasSession(request.getCookie(this.getSessionCookieName()))) {
                return this.getSession(request.getCookie(this.getSessionCookieName()));
            }
        }

        return this.createSession();
    }

    public abstract String getSessionCookieName();

    public abstract boolean hasSession(String key);

    public abstract T getSession(String key);

    protected abstract void addSession(Session session);

    public void removeSession(Session session) {
        this.removeSession(session.getKey());
    }

    public abstract void removeSession(String key);

    public T createSession() {
        String key;
        T session;

        while (true) {
            if (!this.hasSession(key = generateKey())) {
                this.addSession(session = this.factory.createSession(key));
                break;
            }
        }

        return session;
    }

    protected String generateKey() {
        return Long.toHexString(this.random.nextLong());
    }
}
