package org.webbitserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;
import org.webbitserver.HttpRequest;
import org.webbitserver.helpers.QueryParameters;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NettyHttpRequest implements HttpRequest {

    private final FullHttpRequest httpRequest;
    private final Map<String, Object> data = new HashMap<>();
    private final SocketAddress remoteAddress;
    private final Object id;
    private final long timestamp;

    private QueryParameters queryParameters;
    private QueryParameters postParameters;

    public NettyHttpRequest(FullHttpRequest httpRequest, SocketAddress remoteAddress, Object id, long timestamp) {
        this.httpRequest = httpRequest;
        this.remoteAddress = remoteAddress;
        this.id = id;
        this.timestamp = timestamp;
    }

    FullHttpRequest netty() {
        return httpRequest;
    }

    @Override
    public String uri() {
        return httpRequest.getUri();
    }

    @Override
    public NettyHttpRequest uri(String uri) {
        httpRequest.setUri(uri);
        return this;
    }

    @Override
    public String header(CharSequence name) {
        return httpRequest.headers().get(name);
    }

    @Override
    public List<String> headers(String name) {
        return httpRequest.headers().getAll(name);
    }

    @Override
    public boolean hasHeader(String name) {
        return httpRequest.headers().contains(name);
    }

    @Override
    public Set<Cookie> cookies() {
        List<String> headers = headers(HttpHeaders.Names.COOKIE.toString());
        Set<Cookie> cookies = new HashSet<>();
        for (String header : headers) {
            cookies.addAll(CookieDecoder.decode(header));
        }
        return cookies;
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
        return parsedQueryParams().first(key);
    }

    @Override
    public List<String> queryParams(String key) {
        return parsedQueryParams().all(key);
    }

    @Override
    public Set<String> queryParamKeys() {
        return parsedQueryParams().keys();
    }

    @Override
    public String postParam(String key) {
        return parsedPostParams().first(key);
    }

    @Override
    public List<String> postParams(String key) {
        return parsedPostParams().all(key);
    }

    @Override
    public Set<String> postParamKeys() {
        return parsedPostParams().keys();
    }

    private QueryParameters parsedQueryParams() {
        if (queryParameters == null) {
            queryParameters = new QueryParameters(uri(), true);
        }
        return queryParameters;
    }

    private QueryParameters parsedPostParams() {
        if (postParameters == null) {
            postParameters = new QueryParameters(body(), false);
        }
        return postParameters;
    }

    @Override
    public String cookieValue(String name) {
        Cookie cookie = cookie(name);
        return cookie == null ? null : cookie.getValue();
    }

    @Override
    public List<Map.Entry<String, String>> allHeaders() {
        return httpRequest.headers().entries();
    }

    @Override
    public String method() {
        return httpRequest.getMethod().name();
    }

    @Override
    public String body() {
        return httpRequest.content().toString(CharsetUtil.UTF_8); // TODO get charset from request
    }

    @Override
    public byte[] bodyAsBytes() {
        ByteBuf buffer = httpRequest.content();
        byte[] body = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), body);
        return body;
    }

    @Override
    public Map<String, Object> data() {
        return data;
    }

    @Override
    public Object data(String key) {
        return data.get(key);
    }

    @Override
    public NettyHttpRequest data(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @Override
    public Set<String> dataKeys() {
        return data.keySet();
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return remoteAddress + " " + httpRequest.getMethod() + " " + httpRequest.getUri();
    }
}
