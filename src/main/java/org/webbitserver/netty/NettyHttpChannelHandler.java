package org.webbitserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.WebbitException;

import java.util.List;
import java.util.concurrent.Executor;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyHttpChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Executor executor;
    private final List<HttpHandler> httpHandlers;
    private final long timestamp;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final Thread.UncaughtExceptionHandler ioExceptionHandler;
    private final ConnectionHelper connectionHelper;
    private final int maxWebSocketFrameSize;

    public NettyHttpChannelHandler(Executor executor,
                                   List<HttpHandler> httpHandlers,
                                   long timestamp,
                                   Thread.UncaughtExceptionHandler exceptionHandler,
                                   Thread.UncaughtExceptionHandler ioExceptionHandler,
                                   int maxWebSocketFrameSize)
    {
        super(false); // no auto-reelase

        this.executor = executor;
        this.httpHandlers = httpHandlers;
        this.timestamp = timestamp;
        this.exceptionHandler = exceptionHandler;
        this.ioExceptionHandler = ioExceptionHandler;
        this.maxWebSocketFrameSize = maxWebSocketFrameSize;

        connectionHelper = new ConnectionHelper(executor, exceptionHandler, ioExceptionHandler) {
            @Override
            protected void fireOnClose() throws Exception {
                // TODO ?? what does this mean?
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //  && ctx.getAttachment() != IGNORE_REQUEST

        // TODO does this belong here?
        if (!request.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        final NettyHttpRequest nettyHttpRequest =
                new NettyHttpRequest(request, ctx.channel().remoteAddress(), ctx.channel().id(), timestamp);
        DefaultHttpResponse ok_200 = new DefaultHttpResponse(HTTP_1_1, OK);
        final NettyHttpResponse nettyHttpResponse =
                new NettyHttpResponse(ctx, request, ok_200, isKeepAlive(request), exceptionHandler);
        final HttpControl control =
                new NettyHttpControl(httpHandlers.iterator(),
                                     executor,
                                     ctx,
                                     nettyHttpRequest,
                                     nettyHttpResponse,
                                     ok_200,
                                     exceptionHandler,
                                     ioExceptionHandler,
                                     maxWebSocketFrameSize);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    control.nextHandler(nettyHttpRequest, nettyHttpResponse);
                } catch (Exception exception) {
                    exceptionHandler.uncaughtException(Thread.currentThread(),
                                                       WebbitException.fromException(exception, ctx.channel()));
                }
            }
        });
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
        connectionHelper.fireConnectionException(ctx.channel(), cause);

        DefaultFullHttpResponse response =
                new DefaultFullHttpResponse(HTTP_1_1,
                                            INTERNAL_SERVER_ERROR,
                                            Unpooled.copiedBuffer(NettyHttpResponse.getStackTrace(cause),
                                                                  CharsetUtil.US_ASCII));
        response.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/plain");

        ctx.channel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

}
