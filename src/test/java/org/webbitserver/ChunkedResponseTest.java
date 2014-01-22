package org.webbitserver;

import org.junit.After;
import org.junit.Test;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;
import static org.webbitserver.WebServers.createWebServer;
import static org.webbitserver.testutil.HttpClient.httpGet;

public class ChunkedResponseTest {
    private WebServer webServer = createWebServer(12345);

    @After
    public void die() throws Exception {
        webServer.stop();
    }

    @Test
    public void streamingViaChunks() throws Exception {
        webServer.add("/chunked", new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest req, final HttpResponse res, HttpControl control) {
                control.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            res.chunked();
                            nap();
                            // TODO
//                        res.write("chunk1");
                            nap();
                            // TODO
//                        res.write("chunk2");
                            nap();
                            res.end();
                        } catch (InterruptedException e) {
                            res.error(e);
                        }
                    }

                    private void nap() throws InterruptedException {
                        Thread.sleep(10);
                    }
                });
            }
        }).start();


        URLConnection conn = httpGet(webServer, "/chunked");

        assertTrue("should contain chunks", "chunk1chunk2".equals(stringify(conn.getInputStream())));
        assertTrue("should contain Transfer-Encoding header", conn.getHeaderFields().get("Transfer-Encoding") != null);
        assertTrue("should have chunked value in Transfer encoding header",
                   "chunked".equals(conn.getHeaderFields().get("Transfer-Encoding").get(0)));
    }

    private static String stringify(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}