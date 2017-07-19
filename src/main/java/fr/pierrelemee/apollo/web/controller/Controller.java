package fr.pierrelemee.apollo.web.controller;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import java.util.Arrays;
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
                            Object[] args = new Object[1 + method.getDeclaredAnnotation(RouteAnnotation.class).parameters().length];
                            args[0] = requestContext.request();
                            int index = 1;
                            for (String name: method.getDeclaredAnnotation(RouteAnnotation.class).parameters()) {
                                args[index++] = requestContext.pathArgs().get(name);
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
}