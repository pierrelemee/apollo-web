package com.spotify.apollo.web.controller;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.WebRequest;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;
import com.spotify.apollo.web.Cookie;
import com.spotify.apollo.web.Session;
import com.spotify.apollo.web.session.SessionManager;
import okio.ByteString;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Controller implements RouteProvider {
    /**
     * List of cookies to be added in the response
     */
    protected Map<String, Cookie> cookies;
    protected SessionManager<? extends Session> sessionManager;
    protected Renderer renderer;

    public Controller() {
        this(null, null);
    }

    public Controller(SessionManager<? extends Session> sessionManager) {
        this(sessionManager, null);
    }

    public Controller(Renderer renderer) {
        this(null, renderer);
    }

    public Controller(SessionManager<? extends Session> sessionManager, Renderer renderer) {
        this.cookies = new HashMap<>();
        this.sessionManager = sessionManager;
        this.renderer = renderer;
    }

    protected void onRequest(WebRequest request) {
        // Override if needed
    }

    protected Response<ByteString> onResponse(WebRequest request, Response<ByteString> response) {
        // Inject the cookie header to inform client of newly added cookies
        if (this.cookies.size() > 0) {
            String header = String.join(", ", this.cookies.values().stream().map(Cookie::getHeader).collect(Collectors.toList()));
            this.cookies.clear();

            return response.withHeader("Set-Cookie", header);
        }

        return response;
    }

    protected void addCookie(Cookie cookie) {
        this.cookies.put(cookie.getKey(), cookie);
    }

    @Override
    public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
        return Arrays
            .stream(this.getClass().getDeclaredMethods())
            .filter(method -> method.getReturnType() == Response.class)
            .filter(method -> method.isAnnotationPresent(RouteAnnotation.class))
            .map(method -> Route.sync(
                method.getDeclaredAnnotation(RouteAnnotation.class).method(),
                method.getDeclaredAnnotation(RouteAnnotation.class).uri(),
                requestContext -> {
                    WebRequest request = new WebRequest(
                            requestContext.request().uri(),
                            requestContext.pathArgs(),
                            requestContext.request().method(),
                            requestContext.request().payload().isPresent() ? requestContext.request().payload().get() : null, requestContext.request().headers());

                    try {

                        Session session = this.sessionManager.extract(request);

                        if (request.hasCookie(this.sessionManager.getSessionCookieName())) {
                            this.addCookie(Cookie.Builder
                                    .create(this.sessionManager.getSessionCookieName())
                                    .setValue(session.getKey())
                                    .setExpires(15, ChronoUnit.MINUTES)
                                    .build()
                            );
                        }

                        Object[] args = new Object[method.getParameterCount()];
                        for (int i = 0; i < method.getParameterCount(); i++) {
                            if (method.getParameters()[i].getType().equals(WebRequest.class)) {
                                args[i] = request;
                            } else if (method.getParameters()[i].getType().equals(Request.class)) {
                                args[i] = requestContext.request();
                            } else if (this.sessionManager != null && method.getParameters()[i].getType().equals(session.getClass())){
                                args[i] = session;
                            } else {
                                args[i] = null;
                            }
                        }

                        this.onRequest(request);
                        Response r = (Response) method.invoke(this, args);
                        Response<ByteString> response = Response.of(r.status(), ByteString.encodeUtf8(r.payload().isPresent() ? r.payload().get().toString() : "Ok"));
                        response = response.withHeaders(r.headers());
                        response = this.onResponse(request, response);

                        return response;
                    } catch (Throwable t) {
                        t.printStackTrace(System.err);
                        return Response.of(Status.INTERNAL_SERVER_ERROR, t.getMessage());
                    }
                })
            );
    }

    protected Response<String>  redirect(String location) {
        return this.redirect(location, false);
    }

    protected Response<String> redirect(String location, boolean permanent) {
        return Response
            .of(permanent?Status.MOVED_PERMANENTLY:Status.FOUND, "")
            .withHeader("Location", location);

    }

    protected String render(String resource) {
        return this.renderer.render(resource);
    }

    protected String render(String resource, Renderer.Parameter ... parameters) {
        return this.renderer.render(resource, parameters);
    }

    protected String render(String resource, Map<String, Object> parameters) {
        return this.renderer.render(resource, parameters);
    }
}
