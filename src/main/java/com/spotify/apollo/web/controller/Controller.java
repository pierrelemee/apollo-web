package com.spotify.apollo.web.controller;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.WebRequest;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

public abstract class Controller implements RouteProvider {
    /**
     * List of cookies to be added in the response
     */
    protected Map<String, String> cookies;

    public Controller() {
        this.cookies = Collections.emptyMap();
    }

    protected void onRequest(WebRequest request) {
        // Override if needed
    }

    protected void onResponse(Response<String> response, WebRequest request) {
        // Inject all cookie headers to inform client of newly added cookies
        for (String key: this.cookies.keySet()) {
            request.headers().put("Set-Cookie", this.cookies.get(key));
        }
        // reinitialize the list of cookies to add
        this.cookies.clear();
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
                        Response<String> response = (Response) method.invoke(this);
                        this.onResponse(response, request);

                        return response;


                    } catch (Exception e) {
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
