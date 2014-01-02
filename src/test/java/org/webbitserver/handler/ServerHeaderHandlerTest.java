package org.webbitserver.handler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.webbitserver.WebServer;

import java.net.URLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.webbitserver.WebServers.createWebServer;
import static org.webbitserver.testutil.HttpClient.contents;
import static org.webbitserver.testutil.HttpClient.httpGet;

public class ServerHeaderHandlerTest {

    private WebServer webServer;

    @Before
    public void createServer() {
        webServer = createWebServer(59504);
    }

    @After
    public void stopServer() throws Exception {
        webServer.stop();
    }

    @Test
    public void setsHttpServerHeader() throws Exception {
        webServer.add(new ServerHeaderHandler("My Server"))
                .add(new StringHttpHandler("text/plain", "body"))
                .start()
        ;
        URLConnection urlConnection = httpGet(webServer, "/");
        assertEquals("My Server", urlConnection.getHeaderField("Server"));
        assertEquals("body", contents(urlConnection));
    }

    @Test
    public void canBeOverriddenByOtherHandlers() throws Exception {
        webServer.add(new ServerHeaderHandler("My Server"))
                .add(new ServerHeaderHandler("No actually, this is My Server"))
                .add(new StringHttpHandler("text/plain", "body"))
                .start()
        ;
        URLConnection urlConnection = httpGet(webServer, "/");
        assertEquals("No actually, this is My Server", urlConnection.getHeaderField("Server"));
        assertEquals("body", contents(urlConnection));
    }

    @Test
    public void canBeClearedByOtherHandlers() throws Exception {
        webServer.add(new ServerHeaderHandler("My Server"))
                .add(new ServerHeaderHandler(null))
                .add(new StringHttpHandler("text/plain", "body"))
                .start()
        ;
        URLConnection urlConnection = httpGet(webServer, "/");
        assertFalse(urlConnection.getHeaderFields().containsKey("Server"));
        assertEquals("body", contents(urlConnection));
    }
}
