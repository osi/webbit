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
import org.webbitserver.WebbitException;

import java.nio.channels.ClosedChannelException;

public class WebSocketConnectionHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private final Thread.UncaughtExceptionHandler ioExceptionHandler;
    private final NettyWebSocketConnection webSocketConnection;
    private final WebSocketHandler webSocketHandler;
    private final WebSocketServerHandshaker handshaker;

    public WebSocketConnectionHandler(Thread.UncaughtExceptionHandler ioExceptionHandler,
                                      NettyWebSocketConnection webSocketConnection,
                                      WebSocketHandler webSocketHandler,
                                      WebSocketServerHandshaker handshaker)
    {
        this.ioExceptionHandler = ioExceptionHandler;
        this.webSocketConnection = webSocketConnection;
        this.webSocketHandler = webSocketHandler;
        this.handshaker = handshaker;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        webSocketHandler.onClose(webSocketConnection);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getCause() instanceof ClosedChannelException) {
            ctx.close();
        } else {
            ioExceptionHandler.uncaughtException(Thread.currentThread(),
                                                 WebbitException.fromException(cause, ctx.channel()));
        }
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
        } else if (frame instanceof PingWebSocketFrame) {
            webSocketHandler.onPing(webSocketConnection, (PingWebSocketFrame) frame);
        } else if (frame instanceof PongWebSocketFrame) {
            webSocketHandler.onPong(webSocketConnection, (PongWebSocketFrame) frame);
        } else if (frame instanceof TextWebSocketFrame) {
            webSocketHandler.onMessage(webSocketConnection, (TextWebSocketFrame) frame);
        } else if (frame instanceof BinaryWebSocketFrame) {
            webSocketHandler.onMessage(webSocketConnection, (BinaryWebSocketFrame) frame);
        }
    }
}
