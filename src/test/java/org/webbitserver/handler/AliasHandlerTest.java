package org.webbitserver.handler;

import org.junit.After;
import org.junit.Test;
import org.webbitserver.WebServer;

import static org.junit.Assert.assertEquals;
import static org.webbitserver.WebServers.createWebServer;
import static org.webbitserver.testutil.HttpClient.contents;
import static org.webbitserver.testutil.HttpClient.httpGet;

public class AliasHandlerTest {
    private final WebServer webServer = createWebServer(59504);

    @After
    public void die() throws Exception {
        webServer.stop();
    }

    @Test
    public void forwardsAliasedPath() throws Exception {
        webServer
                .add("/tomayto", new AliasHandler("/tomato"))
                .add("/tomato", new StringHttpHandler("text/plain", "body"));
        webServer.start();
        assertEquals("body", contents(httpGet(webServer, "/tomayto")));
    }
}
