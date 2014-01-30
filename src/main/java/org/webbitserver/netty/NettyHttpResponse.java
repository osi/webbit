package org.webbitserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.ReferenceCounted;
import org.webbitserver.WebbitException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Date;

public class NettyHttpResponse implements org.webbitserver.HttpResponse {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private final ChannelHandlerContext ctx;
    private final ReferenceCounted request;
    private final HttpResponse response;
    private final boolean isKeepAlive;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final CompositeByteBuf responseBuffer;
    private Charset charset;

    public NettyHttpResponse(ChannelHandlerContext ctx,
                             ReferenceCounted request,
                             HttpResponse response,
                             boolean isKeepAlive,
                             Thread.UncaughtExceptionHandler exceptionHandler)
    {
        this.ctx = ctx;
        this.request = request;
        this.response = response;
        this.isKeepAlive = isKeepAlive;
        this.exceptionHandler = exceptionHandler;
        this.charset = DEFAULT_CHARSET;
        responseBuffer = ctx.alloc().compositeBuffer();
    }

    @Override
    public NettyHttpResponse charset(Charset charset) {
        this.charset = charset;
        return this;
    }

    @Override
    public Charset charset() {
        return charset;
    }

    @Override
    public NettyHttpResponse status(int status) {
        response.setStatus(HttpResponseStatus.valueOf(status));
        return this;
    }

    @Override
    public NettyHttpResponse chunked() {
        throw new UnsupportedOperationException();
        // TODO chunked
//        response.setHeader(Names.TRANSFER_ENCODING, Values.CHUNKED);
//        response.setChunked(true);
//        ctx.getChannel().write(response);
//        return this;
    }

    @Override
    public int status() {
        return response.getStatus().code();
    }

    @Override
    public NettyHttpResponse header(CharSequence name, CharSequence value) {
        if (value == null) {
            response.headers().remove(name);
        } else {
            response.headers().add(name, value);
        }
        return this;
    }

    @Override
    public NettyHttpResponse header(String name, long value) {
        response.headers().add(name, Long.valueOf(value));
        return this;
    }

    @Override
    public NettyHttpResponse header(CharSequence name, Date value) {
        HttpHeaders.addDateHeader(response, name, value);
        return this;
    }

    @Override
    public boolean containsHeader(CharSequence name) {
        return response.headers().contains(name);
    }

    @Override
    public NettyHttpResponse cookie(Cookie cookie) {
        return header(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));
    }

    @Override
    public NettyHttpResponse content(String content) {
        return content(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(content), charset()));
    }

    @Override
    public NettyHttpResponse content(byte[] content) {
        return content(Unpooled.wrappedBuffer(content));
    }

    @Override
    public NettyHttpResponse content(ByteBuffer buffer) {
        return content(Unpooled.wrappedBuffer(buffer));
    }

    private NettyHttpResponse content(ByteBuf content) {
// TODO chunked-and-this
//        if (response.isChunked()) {
//            throw new UnsupportedOperationException();
//        }
        responseBuffer.addComponent(content);
        responseBuffer.writerIndex(responseBuffer.writerIndex() + content.readableBytes());
        return this;
    }

    @Override
    public NettyHttpResponse error(Throwable error) {
        if (error instanceof TooLongFrameException) {
            response.setStatus(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
        } else {
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        String message = getStackTrace(error);
        header("Content-Type", "text/plain");
        content(message);
        flushResponse();

        exceptionHandler.uncaughtException(Thread.currentThread(),
                                           WebbitException.fromException(error, ctx.channel()));

        return this;
    }

    public static String getStackTrace(Throwable error) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        error.printStackTrace(writer);
        writer.flush();
        return buffer.toString();
    }

    @Override
    public NettyHttpResponse end() {
        flushResponse();
        return this;
    }

    private void flushResponse() {
        if (!request.release()) {
            // TODO potential leak!
        }

        try {
            // TODO support explicit content length in case of chunked?
            HttpHeaders.setContentLength(response, responseBuffer.readableBytes());

            ctx.write(response); // start of the HTTP message


//            if (response.isChunked()) {
//                ctx.getChannel()
//                        .write(new DefaultHttpChunk(ChannelBuffers.EMPTY_BUFFER));
//            } else {
            ctx.write(responseBuffer);
//            }

            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            if (!isKeepAlive) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Exception e) {
            exceptionHandler.uncaughtException(Thread.currentThread(),
                                               WebbitException.fromException(e, ctx.channel()));
        }
    }
}
