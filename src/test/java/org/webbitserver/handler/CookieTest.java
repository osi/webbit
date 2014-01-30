package org.webbitserver.handler;

import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import org.junit.After;
import org.junit.Test;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.WebServer;

import java.net.HttpCookie;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.webbitserver.WebServers.createWebServer;
import static org.webbitserver.testutil.HttpClient.contents;
import static org.webbitserver.testutil.HttpClient.httpGet;

public class CookieTest {
    private final WebServer webServer = createWebServer(59504);

    @After
    public void die() throws Exception {
        webServer.stop();
    }

    @Test
    public void setsOneOutboundCookie() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                response.cookie(new DefaultCookie("a", "b")).end();
            }
        }).start();
        URLConnection urlConnection = httpGet(webServer, "/");
        List<Cookie> cookies = cookies(urlConnection);
        assertEquals(1, cookies.size());
        assertEquals("a", cookies.get(0).getName());
        assertEquals("b", cookies.get(0).getValue());
    }

    @Test
    public void setsTwoOutboundCookies() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                response.cookie(new DefaultCookie("a", "b")).cookie(new DefaultCookie("c", "d")).end();
            }
        }).start();
        URLConnection urlConnection = httpGet(webServer, "/");
        List<Cookie> cookies = cookies(urlConnection);
        assertEquals(2, cookies.size());
        assertEquals("a", cookies.get(0).getName());
        assertEquals("b", cookies.get(0).getValue());
        assertEquals("c", cookies.get(1).getName());
        assertEquals("d", cookies.get(1).getValue());
    }

    @Test
    public void doesntSetMaxAgeIfUnspecified() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                response.cookie(new DefaultCookie("a", "b")).end();
            }
        }).start();
        URLConnection urlConnection = httpGet(webServer, "/");
        List<Cookie> cookies = cookies(urlConnection);
        assertEquals(Long.MIN_VALUE, cookies.get(0).getMaxAge());
    }

    @Test
    public void parsesOneInboundCookie() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                String body = "Your cookie value: " + request.cookieValue("someName");
                response.header("Content-Length", body.length())
                        .content(body)
                        .end();
            }
        }).start();
        URLConnection urlConnection = httpGet(webServer, "/");
        urlConnection.addRequestProperty("Cookie", new HttpCookie("someName", "someValue").toString());
        assertEquals("Your cookie value: someValue", contents(urlConnection));
    }

    @Test
    public void parsesThreeInboundCookiesInTwoHeaders() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                String body = "Your cookies:";
                List<Cookie> cookies = sort(new ArrayList<>(request.cookies()));
                for (Cookie cookie : cookies) {
                    body += " " + cookie.getName() + "=" + cookie.getValue();
                }
                response.header("Content-Length", body.length())
                        .content(body)
                        .end();
            }
        }).start();
        URLConnection urlConnection = httpGet(webServer, "/");
        urlConnection.addRequestProperty("Cookie", new HttpCookie("a", "b").toString());
        urlConnection.addRequestProperty("Cookie",
                                         new HttpCookie("c", "d").toString() + "; " + new HttpCookie("e",
                                                                                                     "f").toString());
        assertEquals("Your cookies: a=b c=d e=f", contents(urlConnection));
    }

    @Test
    public void parsesCookiesWithExtraAttributes() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                StringBuilder body = new StringBuilder("Your cookies:");
                List<Cookie> cookies = sort(new ArrayList<>(request.cookies()));
                for (Cookie cookie : cookies) {
                    String path = "";
                    if (cookie.getPath() != null) {
                        path = "; path:" + cookie.getPath();
                    }
                    body.append(" ")
                            .append(cookie.getName())
                            .append("=")
                            .append(cookie.getValue())
                            .append(path)
                            .append("|");
                }
                response.header("Content-Length", body.length())
                        .content(body)
                        .end();
            }
        }).start();
        URLConnection urlConnection = httpGet(webServer, "/");
        Cookie t = new DefaultCookie("a", "b");
        t.setMaxAge(5000);
        t.setSecure(true);
        t.setPath("/path");
        urlConnection.addRequestProperty("Cookie", ClientCookieEncoder.encode(t));
        String s = new HttpCookie("c", "d").toString();
        urlConnection.addRequestProperty("Cookie", s + "; " + new HttpCookie("e", "f").toString());
        assertEquals(
                "Your cookies: a=b; path:/path| c=d| e=f|",
                contents(urlConnection));
    }

    @Test
    public void behavesWellWhenThereAreNoInboundCookies() throws Exception {
        webServer.add(new HttpHandler() {
            @Override
            public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control)
                    throws Exception
            {
                String body = "Cookie count:" + request.cookies().size();
                response.header("Content-Length", body.length())
                        .content(body)
                        .end();
            }
        }).start();
        URLConnection urlConnection = httpGet(webServer, "/");
        assertEquals("Cookie count:0", contents(urlConnection));
    }

    // You wouldn't have thought it was that convoluted, but it is.
    private List<Cookie> cookies(URLConnection urlConnection) {
        List<Cookie> cookies = new ArrayList<>();
        Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
        for (Map.Entry<String, List<String>> header : headerFields.entrySet()) {
            if ("Set-Cookie".equals(header.getKey())) {
                List<String> value = header.getValue();
                for (String cookie : value) {
                    //since this processing is per header, there is only one cookie to parse
                    Cookie nettCookie = CookieDecoder.decode(cookie).iterator().next();
                    cookies.add(nettCookie);
                }
            }
        }
        return sort(cookies);
    }

    private List<Cookie> sort(List<Cookie> cookies) {
        Collections.sort(cookies, new Comparator<Cookie>() {
            @Override
            public int compare(Cookie a, Cookie b) {
                return a.getName().compareTo(b.getName());
            }
        });
        return cookies;
    }
}
