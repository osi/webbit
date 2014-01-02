package org.webbitserver.stub;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import org.webbitserver.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Implementation of HttpResponse that is easy to construct manually, and inspect results.
 * Useful for testing.
 */
public class StubHttpResponse implements HttpResponse {

    private Charset charset = Charset.forName("UTF-8");
    private int status = 200;
    private final HttpHeaders headers = new DefaultHttpHeaders();
    private Throwable error;
    private boolean ended;
    private final ByteArrayOutputStream contents = new ByteArrayOutputStream();
    private final List<Cookie> cookies = new ArrayList<>();


    @Override
    public StubHttpResponse charset(Charset charset) {
        this.charset = charset;
        return this;
    }

    @Override
    public Charset charset() {
        return charset;
    }

    @Override
    public StubHttpResponse chunked() {
        header(Names.TRANSFER_ENCODING, Values.CHUNKED);
        return this;
    }

    @Override
    public StubHttpResponse status(int status) {
        this.status = status;
        return this;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public StubHttpResponse header(CharSequence name, CharSequence value) {
        if (value == null) {
            headers.remove(name);
        } else {
            headers.set(name, value);
        }
        return this;
    }

    @Override
    public StubHttpResponse header(String name, long value) {
        headers.set(name, Long.valueOf(value));
        return this;
    }

    @Override
    public StubHttpResponse header(CharSequence name, Date value) {
        headers.set(name, value);
        return this;
    }

    @Override
    public StubHttpResponse cookie(Cookie cookie) {
        cookies.add(cookie);
        return this;
    }

    public StubHttpResponse cookie(String name, String value) {
        return cookie(new DefaultCookie(name, value));
    }

    public String header(String name) {
        return headers.get(name);
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return headers.contains(name);
    }

    @Override
    public StubHttpResponse content(String content) {
        return content(content.getBytes(charset));
    }

    @Override
    public StubHttpResponse content(byte[] content) {
        try {
            contents.write(content);
        } catch (IOException e) {
            throw new Error(e);
        }
        return this;
    }

    @Override
    public StubHttpResponse content(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            contents.write(buffer.get());
        }
        return this;
    }

    public byte[] contents() {
        return contents.toByteArray();
    }

    public String contentsString() {
        return new String(contents(), charset);
    }

    @Override
    public StubHttpResponse error(Throwable error) {
        this.error = error;
        status = 500;
        String message = error.toString();
        this.content(message);
        header("Content-Type", "text/plain");
        header("Content-Length", message.length());
        ended = true;
        return this;
    }

    public Throwable error() {
        return error;
    }

    @Override
    public StubHttpResponse end() {
        ended = true;
        return this;
    }

    public boolean ended() {
        return ended;
    }

    public List<Cookie> cookies() {
        return cookies;
    }

    @Override
    public String toString() {
        return "StubHttpResponse{" +
               "charset=" + charset +
               ", status=" + status +
               ", headers=" + headers +
               ", error=" + error +
               ", ended=" + ended +
               ", contents=" + contentsString() +
               '}';
    }
}
