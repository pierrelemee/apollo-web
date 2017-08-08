package com.spotify.apollo;

import junit.framework.TestCase;
import okio.ByteString;
import org.junit.Test;

import java.util.Collections;

public class WebRequestTest extends TestCase {

    @Test
    public void testPostRequest() throws Exception {
        WebRequest request = new WebRequest(
            "http;//www.example.com",
            "POST",
            ByteString.of("foo=bar".getBytes()),
            Collections.emptyMap()
        );
        assertTrue(request.parameter("foo").isPresent());
        assertEquals("bar", request.parameter("foo").get());
        assertTrue(request.post().containsKey("foo"));
        assertEquals("bar", request.post().get("foo").get(0));
    }
}
