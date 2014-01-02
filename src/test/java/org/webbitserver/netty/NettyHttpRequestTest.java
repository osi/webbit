package org.webbitserver.netty;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NettyHttpRequestTest {

    @Test
    public void decodesQueryParams() {
        FullHttpRequest httpRequest =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "http://example.com/?foo=bar");
        NettyHttpRequest nhr = new NettyHttpRequest(httpRequest, null, null, 0L);
        assertEquals(nhr.queryParam("foo"), "bar");
    }

    @Test
    public void decodesQueryParamsContainingEncodedEquals() {
        FullHttpRequest httpRequest =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "http://example.com/?foo=a%2Bb%3Dc");
        NettyHttpRequest nhr = new NettyHttpRequest(httpRequest, null, null, 0L);
        assertEquals(nhr.queryParam("foo"), "a+b=c");
    }
}