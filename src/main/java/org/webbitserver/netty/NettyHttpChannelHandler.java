package org.webbitserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.WebbitException;

import java.nio.channels.ClosedChannelException;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyHttpChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final List<HttpHandler> httpHandlers;
    private final long timestamp;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final Thread.UncaughtExceptionHandler ioExceptionHandler;
    private final int maxWebSocketFrameSize;

    public NettyHttpChannelHandler(List<HttpHandler> httpHandlers,
                                   long timestamp,
                                   Thread.UncaughtExceptionHandler exceptionHandler,
                                   Thread.UncaughtExceptionHandler ioExceptionHandler,
                                   int maxWebSocketFrameSize)
    {
        this.httpHandlers = httpHandlers;
        this.timestamp = timestamp;
        this.exceptionHandler = exceptionHandler;
        this.ioExceptionHandler = ioExceptionHandler;
        this.maxWebSocketFrameSize = maxWebSocketFrameSize;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //  && ctx.getAttachment() != IGNORE_REQUEST

        // TODO does this belong here?
        if (!request.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        NettyHttpRequest nettyHttpRequest =
                new NettyHttpRequest(request, ctx.channel().remoteAddress(), ctx.channel().id(), timestamp);
        DefaultHttpResponse ok_200 = new DefaultHttpResponse(HTTP_1_1, OK);
        NettyHttpResponse nettyHttpResponse =
                new NettyHttpResponse(ctx, request, ok_200, isKeepAlive(request), exceptionHandler);
        HttpControl control =
                new NettyHttpControl(httpHandlers.iterator(),
                                     ctx,
                                     nettyHttpRequest,
                                     nettyHttpResponse,
                                     ok_200,
                                     ioExceptionHandler,
                                     maxWebSocketFrameSize);

        control.nextHandler(nettyHttpRequest, nettyHttpResponse);
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getCause() instanceof ClosedChannelException) {
            ctx.close();
            return;
        }

        Thread thread = Thread.currentThread();
        Channel channel = ctx.channel();
        ioExceptionHandler.uncaughtException(thread, WebbitException.fromException(cause, channel));

        HttpResponseStatus status;
        ByteBuf content;

        if (cause instanceof TooLongFrameException) {
            status = HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE;
            content = Unpooled.copiedBuffer(cause.getMessage(), CharsetUtil.US_ASCII);
            // We cannot have the compressor in the outbound pipeline in this case because
            // it never saw the incoming request, and thus never had a chance to record the Accept-Encoding header
            ctx.pipeline().remove("compressor");
        } else {
            status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
            content = Unpooled.copiedBuffer(NettyHttpResponse.getStackTrace(cause), CharsetUtil.US_ASCII);
        }

        DefaultFullHttpResponse response =
                new DefaultFullHttpResponse(HTTP_1_1, status, content);
        response.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/plain");

        channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
