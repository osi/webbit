package org.webbitserver.handler;

import org.junit.After;
import org.junit.Test;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.WebServer;

import java.net.HttpURLConnection;

import static org.junit.Assert.assertEquals;
import static org.webbitserver.WebServers.createWebServer;
import static org.webbitserver.testutil.HttpClient.contents;
import static org.webbitserver.testutil.HttpClient.decompressContents;
import static org.webbitserver.testutil.HttpClient.httpGetAcceptCompressed;
import static org.webbitserver.testutil.HttpClient.httpPostCompressed;

public class CompressionTest {

    private final WebServer webServer = createWebServer(59504);

    private final String content =
            "Very short string for which there is no real point in compressing, but we're going to do it anyway.";

    @After
    public void die() throws Exception {
        webServer.stop();
    }

    @Test
    public void compressedPostIsUncompressedProperly() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                response.content(request.body()).end();
            }
        }).start();
        String result = contents(httpPostCompressed(webServer, "/", content));
        assertEquals(content, result);
    }

    @Test
    public void compressedResponseIsSentProperly() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                response.content(content).end();
            }
        }).start();
        HttpURLConnection urlConnection = (HttpURLConnection) httpGetAcceptCompressed(webServer, "/");
        String result = decompressContents(urlConnection);
        assertEquals(content, result);
        assertEquals("gzip", urlConnection.getContentEncoding());
    }

}
