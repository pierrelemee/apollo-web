package com.spotify.apollo.web.controller;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.WebRequest;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;
import sun.misc.GC;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public abstract class Controller implements RouteProvider {

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
                    try {
                        if (method.getParameterCount() > 0) {
                            Object[] args = new Object[method.getParameterCount()];
                            for (int i = 0; i < method.getParameterCount(); i++) {
                                if (method.getParameters()[i].getType().equals(WebRequest.class)) {
                                    args[i] = new WebRequest(
                                            requestContext.request().uri(),
                                            requestContext.pathArgs(),
                                            requestContext.request().method(),
                                            requestContext.request().payload().isPresent() ? requestContext.request().payload().get() : null, requestContext.request().headers());
                                } else if (method.getParameters()[i].getType().equals(Request.class)) {
                                    args[i] = requestContext.request();
                                } else {
                                    args[i] = null;
                                }
                            }

                            return (Response) method.invoke(this, args);
                        }

                        return (Response) method.invoke(this);
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
