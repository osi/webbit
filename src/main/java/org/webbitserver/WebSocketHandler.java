package org.webbitserver;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * It is not necessary to release any of the netty messages as long as they are not retained further.
 */
public interface WebSocketHandler {
    void onOpen(WebSocketConnection connection);

    /**
     * Called when a connection is closed.
     *
     * @param connection the connection that was closed. Beware that the connection will be null if this handler is used in a WebSocketConnection that fails to connect.
     * @throws Exception
     */
    void onClose(WebSocketConnection connection) throws Exception;

    void onMessage(WebSocketConnection connection, TextWebSocketFrame msg) throws Exception;

    void onMessage(WebSocketConnection connection, BinaryWebSocketFrame msg) throws Exception;

    void onPing(WebSocketConnection connection, PingWebSocketFrame frame) throws Exception;

    void onPong(WebSocketConnection connection, PongWebSocketFrame frame) throws Exception;
}
