package fr.pierrelemee.apollo.web;

import com.spotify.apollo.httpservice.HttpService;
import fr.pierrelemee.apollo.web.controller.AssetsProvider;
import fr.pierrelemee.apollo.web.controller.Controller;

import java.util.List;

public abstract class WebApplication {

    protected abstract String getName();

    protected abstract AssetsProvider getAssetsProvider();

    protected abstract List<Controller> getControllers() throws Exception;

    public void start() throws Exception {
        List<Controller> controllers = this.getControllers();
        HttpService.boot(HttpService.usingAppInit(environment -> {
            for (Controller controller: controllers) {
                environment.routingEngine().registerAutoRoutes(controller);
            }

            AssetsProvider assetsProvider;
            if (null != (assetsProvider = this.getAssetsProvider())) {
                environment.routingEngine().registerAutoRoutes(assetsProvider);
            }
        }, getName()).withCliHelp(true).build());
    }
}
