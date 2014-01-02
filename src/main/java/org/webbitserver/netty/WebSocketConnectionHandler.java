package org.webbitserver.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.webbitserver.WebSocketHandler;

import java.util.concurrent.Executor;

public class WebSocketConnectionHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private final Executor executor;
    private final NettyWebSocketConnection webSocketConnection;
    private final WebSocketHandler webSocketHandler;
    private final WebSocketServerHandshaker handshaker;
    private final ConnectionHelper connectionHelper;

    public WebSocketConnectionHandler(
            Executor executor,
            Thread.UncaughtExceptionHandler exceptionHandler,
            Thread.UncaughtExceptionHandler ioExceptionHandler,
            final NettyWebSocketConnection webSocketConnection,
            final WebSocketHandler webSocketHandler,
            WebSocketServerHandshaker handshaker)
    {
        super(false); // no auto-release
        this.executor = executor;
        this.webSocketConnection = webSocketConnection;
        this.webSocketHandler = webSocketHandler;
        this.handshaker = handshaker;
        this.connectionHelper = new ConnectionHelper(executor, exceptionHandler, ioExceptionHandler) {
            @Override
            protected void fireOnClose() throws Throwable {
                webSocketHandler.onClose(webSocketConnection);
            }
        };
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionHelper.fireOnClose(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        connectionHelper.fireConnectionException(ctx.channel(), cause);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, final WebSocketFrame frame) throws Exception {
        Thread.UncaughtExceptionHandler exceptionHandlerWithContext =
                connectionHelper.webbitExceptionWrappingExceptionHandler(ctx.channel());

        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        executor.execute(new CatchingRunnable(exceptionHandlerWithContext) {
            @Override
            protected void go() throws Throwable {
                try {
                    if (frame instanceof PingWebSocketFrame) {
                        webSocketHandler.onPing(webSocketConnection, (PingWebSocketFrame) frame);
                    } else if (frame instanceof PongWebSocketFrame) {
                        webSocketHandler.onPong(webSocketConnection, (PongWebSocketFrame) frame);
                    } else if (frame instanceof TextWebSocketFrame) {
                        webSocketHandler.onMessage(webSocketConnection, (TextWebSocketFrame) frame);
                    } else if (frame instanceof BinaryWebSocketFrame) {
                        webSocketHandler.onMessage(webSocketConnection, (BinaryWebSocketFrame) frame);
                    }
                } finally {
                    frame.release();
                }
            }
        });
    }
}
