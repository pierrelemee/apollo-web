package com.spotify.apollo.web.controller;

import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;

import java.util.Map;

public class Router {

    protected Map<String, Route<? extends AsyncHandler<?>>> routes;

    public void register(String name, Route route) {}
}
