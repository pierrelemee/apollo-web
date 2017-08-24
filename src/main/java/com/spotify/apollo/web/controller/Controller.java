package com.spotify.apollo.web.controller;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.WebRequest;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;
import com.spotify.apollo.web.Cookie;
import okio.ByteString;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public abstract class Controller implements RouteProvider {
    /**
     * List of cookies to be added in the response
     */
    protected Map<String, Cookie> cookies;

    public Controller() {
        this.cookies = new HashMap<>();
    }

    protected void onRequest(WebRequest request) {
        // Override if needed
    }

    protected Response<ByteString> onResponse(WebRequest request, Response<ByteString> response) {
        // Inject all cookie headers to inform client of newly added cookies
        for (String key: this.cookies.keySet()) {
            response = response.withHeader("Set-Cookie", this.cookies.get(key).getHeader());
        }
        // reinitialize the list of cookies to add
        this.cookies.clear();

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

                        Object[] args = new Object[method.getParameterCount()];
                        for (int i = 0; i < method.getParameterCount(); i++) {
                            if (method.getParameters()[i].getType().equals(WebRequest.class)) {
                                args[i] = request;
                            } else if (method.getParameters()[i].getType().equals(Request.class)) {
                                args[i] = requestContext.request();
                            } else {
                                args[i] = null;
                            }
                        }

                        this.onRequest(request);
                        Response r = (Response) method.invoke(this, args);

                        Response<ByteString> response =
                            Response.of(r.status(), ByteString.encodeUtf8(r.payload().isPresent() ? r.payload().get().toString() : "Ok"));
                        response = this.onResponse(request, response);

                        return response;


                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        return Response.of(Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
}
