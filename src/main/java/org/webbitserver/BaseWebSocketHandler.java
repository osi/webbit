package org.webbitserver;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * Base implementation that does nothing, except for automatically calling
 * {@link WebSocketConnection#pong(byte[])} when {@link WebSocketHandler#onPing(WebSocketConnection, io.netty.handler.codec.http.websocketx.PingWebSocketFrame)}
 * receives a ping.
 */
public class BaseWebSocketHandler implements WebSocketHandler {
    @Override
    public void onOpen(WebSocketConnection connection) throws Exception {
    }

    @Override
    public void onClose(WebSocketConnection connection) throws Exception {
    }

    @Override
    public void onMessage(WebSocketConnection connection, TextWebSocketFrame msg) throws Throwable {
    }

    @Override
    public void onMessage(WebSocketConnection connection, BinaryWebSocketFrame msg) throws Throwable {
    }

    @Override
    public void onPing(WebSocketConnection connection, PingWebSocketFrame frame) throws Throwable {
        connection.pong(new PongWebSocketFrame(frame.content()).retain());
    }

    @Override
    public void onPong(WebSocketConnection connection, PongWebSocketFrame frame) throws Throwable {
    }
}
