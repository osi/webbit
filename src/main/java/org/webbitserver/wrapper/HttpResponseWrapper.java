package org.webbitserver.wrapper;

import io.netty.handler.codec.http.Cookie;
import org.webbitserver.HttpResponse;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

public class HttpResponseWrapper implements HttpResponse {

    private HttpResponse response;

    public HttpResponseWrapper(HttpResponse response) {
        this.response = response;
    }

    public HttpResponse underlyingResponse() {
        return response;
    }

    public HttpResponseWrapper underlyingResponse(HttpResponse response) {
        this.response = response;
        return this;
    }

    public HttpResponse originalResponse() {
        if (response instanceof HttpResponseWrapper) {
            HttpResponseWrapper wrapper = (HttpResponseWrapper) response;
            return wrapper.originalResponse();
        } else {
            return response;
        }
    }

    @Override
    public HttpResponseWrapper charset(Charset charset) {
        response.charset(charset);
        return this;
    }

    @Override
    public Charset charset() {
        return response.charset();
    }

    @Override
    public HttpResponseWrapper chunked() {
        response.chunked();
        return this;
    }

    @Override
    public HttpResponseWrapper status(int status) {
        response.status(status);
        return this;
    }

    @Override
    public int status() {
        return response.status();
    }

    @Override
    public HttpResponseWrapper header(CharSequence name, CharSequence value) {
        response.header(name, value);
        return this;
    }

    @Override
    public HttpResponseWrapper header(String name, long value) {
        response.header(name, value);
        return this;
    }

    @Override
    public HttpResponseWrapper cookie(Cookie cookie) {
        response.cookie(cookie);
        return this;
    }

    @Override
    public HttpResponseWrapper content(CharSequence content) {
        response.content(content);
        return this;
    }

    @Override
    public HttpResponseWrapper content(byte[] content) {
        response.content(content);
        return this;
    }

    @Override
    public HttpResponseWrapper content(ByteBuffer buffer) {
        response.content(buffer);
        return this;
    }

    @Override
    public HttpResponseWrapper error(Throwable error) {
        response.error(error);
        return this;
    }

    @Override
    public HttpResponseWrapper end() {
        response.end();
        return this;
    }

    @Override
    public HttpResponseWrapper header(CharSequence name, Date value) {
        response.header(name, value);
        return this;
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return response.containsHeader(name);
    }
}
