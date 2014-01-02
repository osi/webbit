package org.webbitserver.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.webbitserver.WebSocketConnection;

import java.util.concurrent.Executor;

public class NettyWebSocketConnection extends AbstractHttpConnection implements WebSocketConnection {

    private final String version;

    public NettyWebSocketConnection(Executor executor,
                                    NettyHttpRequest nettyHttpRequest,
                                    ChannelHandlerContext ctx,
                                    String version)
    {
        super(ctx, nettyHttpRequest, executor);
        this.version = version;
    }

    @Override
    public NettyWebSocketConnection send(TextWebSocketFrame frame) {
        writeMessage(frame);
        return this;
    }

    @Override
    public NettyWebSocketConnection send(BinaryWebSocketFrame frame) {
        writeMessage(frame);
        return this;
    }

    @Override
    public NettyWebSocketConnection ping(PingWebSocketFrame frame) {
        writeMessage(frame);
        return this;
    }

    @Override
    public NettyWebSocketConnection pong(PongWebSocketFrame frame) {
        writeMessage(frame);
        return this;
    }

    @Override
    public NettyWebSocketConnection send(String message) {
        writeMessage(new TextWebSocketFrame(message));
        return this;
    }

    @Override
    public NettyWebSocketConnection send(byte[] message) {
        writeMessage(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(message)));
        return this;
    }

    @Override
    public NettyWebSocketConnection send(byte[] message, int offset, int length) {
        writeMessage(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(message, offset, length)));
        return this;
    }

    @Override
    public NettyWebSocketConnection ping(byte[] message) {
        writeMessage(new PingWebSocketFrame(Unpooled.wrappedBuffer(message)));
        return this;
    }

    @Override
    public NettyWebSocketConnection pong(byte[] message) {
        writeMessage(new PongWebSocketFrame(Unpooled.wrappedBuffer(message)));
        return this;
    }

    @Override
    public NettyWebSocketConnection close() {
        return close(1000, "");
    }

    @Override
    public NettyWebSocketConnection close(int status, String reason) {
        ctx.channel().writeAndFlush(new CloseWebSocketFrame(status, reason)).addListener(ChannelFutureListener.CLOSE);
        return this;
    }

    @Override
    public NettyWebSocketConnection data(String key, Object value) {
        putData(key, value);
        return this;
    }

    @Override
    public String version() {
        return version;
    }

}
