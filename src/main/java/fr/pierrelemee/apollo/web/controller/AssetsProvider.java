package fr.pierrelemee.apollo.web.controller;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.stream.Stream;

public class AssetsProvider implements RouteProvider {

    private static final String DEFAULT_RESOURCE_PATH = "/web";

    protected Path path;

    public AssetsProvider() {
        this(DEFAULT_RESOURCE_PATH);
    }

    public AssetsProvider(String resource) {
        try {
            URI uri = getClass().getResource(resource).toURI();
            if (uri.getScheme().equals("jar")) {
                FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                this.path = fileSystem.getPath(resource);
            } else {
                this.path = Paths.get(uri);
            }
        } catch (IOException ioe) {
        } catch (URISyntaxException ioe) {
            ioe.printStackTrace();
        }
    }

    private InputStreamReader getResourceReader(String resource ) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream(resource);

        return in != null ? new InputStreamReader(in) : null;
    }

    private String getResourceContent(Path path) {
        System.out.println((DEFAULT_RESOURCE_PATH + path.toString().substring(this.path.toString().length())).substring(1));
        InputStreamReader in = getResourceReader( (DEFAULT_RESOURCE_PATH + path.toString().substring(this.path.toString().length())).substring(1));

        if (in == null) {
            return path.toString();
        }

        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(in);
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException ioe) {
        }

        return buffer.toString();
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
        try {
            return Files.walk(this.path)
                    .filter(Files::isRegularFile)
                    .map(path -> Route
                            .sync("GET", path.toString().substring(this.path.toString().length()), requestContext ->
                                    Response.of(Status.OK, getResourceContent(path))
                            )
                    );
        } catch (IOException ioe) {
            return Stream.empty();
        }
    }
}

