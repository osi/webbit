package org.webbitserver.stub;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import org.webbitserver.HttpRequest;
import org.webbitserver.helpers.QueryParameters;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of HttpRequest that is easy to construct manually and populate.
 * Useful for testing.
 */
public class StubHttpRequest extends StubDataHolder implements HttpRequest {

    private String uri = "/";
    private String method = "GET";
    private final List<Map.Entry<String, String>> headers = new ArrayList<>();
    private SocketAddress remoteAddress = new InetSocketAddress("localhost", 0);
    private Object id = "StubID";
    private long timestamp = 0;
    private String body;

    public StubHttpRequest() {
    }

    public StubHttpRequest(String uri) {
        this.uri = uri;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public StubHttpRequest uri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String header(CharSequence name) {
        for (Map.Entry<String, String> header : headers) {
            if (header.getKey().equals(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean hasHeader(String name) {
        for (Map.Entry<String, String> header : headers) {
            if (header.getKey().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Cookie> cookies() {
        return CookieDecoder.decode(header(HttpHeaders.Names.COOKIE));
    }

    @Override
    public Cookie cookie(String name) {
        for (Cookie cookie : cookies()) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    @Override
    public String queryParam(String key) {
        return new QueryParameters(uri()).first(key);
    }

    @Override
    public List<String> queryParams(String key) {
        return new QueryParameters(uri()).all(key);
    }

    @Override
    public Set<String> queryParamKeys() {
        return new QueryParameters(uri()).keys();
    }

    @Override
    public String postParam(String key) {
        return new QueryParameters(body()).first(key);
    }

    @Override
    public List<String> postParams(String key) {
        return new QueryParameters(body()).all(key);
    }

    @Override
    public Set<String> postParamKeys() {
        return new QueryParameters(body()).keys();
    }

    @Override
    public String cookieValue(String name) {
        Cookie cookie = cookie(name);
        return cookie == null ? null : cookie.getValue();
    }

    @Override
    public List<String> headers(String name) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> header : headers) {
            if (header.getKey().equals(name)) {
                result.add(header.getValue());
            }
        }
        return result;
    }

    @Override
    public List<Map.Entry<String, String>> allHeaders() {
        return headers;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public String body() {
        return body;
    }

    @Override
    public byte[] bodyAsBytes() {
        return body.getBytes();
    }

    public StubHttpRequest body(String body) {
        this.body = body;
        return this;
    }

    public StubHttpRequest method(String method) {
        this.method = method;
        return this;
    }

    public StubHttpRequest header(String name, String value) {
        headers.add(new AbstractMap.SimpleEntry<>(name, value));
        return this;
    }

    @Override
    public StubHttpRequest data(String key, Object value) {
        super.data(key, value);
        return this;
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public Object id() {
        return id;
    }

    public StubHttpRequest id(Object id) {
        this.id = id;
        return this;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    public StubHttpRequest timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public StubHttpRequest remoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }
}
