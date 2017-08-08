package com.spotify.apollo;

import okio.ByteString;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WebRequest implements Request {

    private static final String POST = "POST";

    private final String method;
    private final String uri;
    private final Map<String, List<String>> get;
    private final Map<String, List<String>> post;
    private final Map<String, String> headers;
    private final Optional<ByteString> payload;

    public WebRequest(String uri) {
        this(uri, RequestValue.GET);
    }

    public WebRequest(String uri, String method) {
        this(uri, method, null);
    }

    public WebRequest(String uri, String method, ByteString payload) {
        this(uri, method, payload, Collections.emptyMap());
    }

    public WebRequest(String uri, String method, ByteString payload, Map<String, String> headers) {
        this.method = method;
        this.uri = uri;
        this.payload = Optional.of(payload != null ? payload : ByteString.EMPTY);
        if (method.equalsIgnoreCase(POST)) {
            this.post = new QueryStringDecoder(this.payload().isPresent() ? this.payload().get().utf8() : "", false).parameters();
            this.get = Collections.emptyMap();
        } else {
            this.get = new QueryStringDecoder(this.uri).parameters();
            this.post = Collections.emptyMap();
        }
        this.headers = headers;
    }

    @Override
    public String method() {
        return this.method;
    }

    @Override
    public String uri() {
        return this.uri;
    }

    public Map<String, List<String>> get() {
        return this.get;
    }
    public Map<String, List<String>> post() {
        return this.post;
    }

    @Override
    public Map<String, List<String>> parameters() {
        return this.method.equalsIgnoreCase(POST) ? this.post : this.get;
    }

    @Override
    public Map<String, String> headers() {
        return this.headers;
    }

    @Override
    public Optional<String> service() {
        return null;
    }

    @Override
    public Optional<ByteString> payload() {
        return this.payload;
    }

    @Override
    public WebRequest withUri(String uri) {
        return new WebRequest(uri);
    }

    @Override
    public WebRequest withService(String service) {
        return null;
    }

    @Override
    public WebRequest withHeader(String name, String value) {
        WebRequest request = new WebRequest(this.uri(), this.method());
        request.headers.put(name, value);
        return request;
    }

    @Override
    public WebRequest withHeaders(Map<String, String> additionalHeaders) {
        return null;
    }

    @Override
    public WebRequest clearHeaders() {
        return new WebRequest(this.uri(), this.method());
    }

    @Override
    public WebRequest withPayload(ByteString payload) {
        return new WebRequest(uri(), method(), payload);
    }
}
