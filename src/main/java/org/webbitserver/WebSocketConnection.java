package org.webbitserver;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public interface WebSocketConnection extends HttpConnection {

    /**
     * Sends a text frame
     *
     * @param message frame payload
     * @return this
     */
    WebSocketConnection send(String message);

    /**
     * Sends a binary frame
     *
     * @param message frame payload
     * @return this
     */
    WebSocketConnection send(byte[] message);

    /**
     * Sends a binary frame
     *
     * @param message frame payload
     * @param offset  The offset within the array of the first byte to be written; must be non-negative and no larger than <code>message.length</code>
     * @param length  The maximum number of bytes to be written to the given array; must be non-negative and no larger than <code>message.length - offset</code>
     * @return this
     */
    WebSocketConnection send(byte[] message, int offset, int length);

    /**
     * Sends a ping frame
     *
     * @param message the payload of the ping
     * @return this
     */
    WebSocketConnection ping(byte[] message);

    /**
     * Sends a pong frame
     *
     * @param message the payload of the ping
     * @return this
     */
    WebSocketConnection pong(byte[] message);

    WebSocketConnection send(TextWebSocketFrame frame);

    WebSocketConnection send(BinaryWebSocketFrame frame);

    WebSocketConnection ping(PingWebSocketFrame frame);

    WebSocketConnection pong(PongWebSocketFrame frame);

    /**
     * @return the WebSocket protocol version
     */
    String version();

    // Override methods to provide more specific return type.

    /**
     * Close the web socket with status 1000 and no reason
     *
     * @return
     */
    @Override
    WebSocketConnection close();

    WebSocketConnection close(int status, String reason);

    @Override
    WebSocketConnection data(String key, Object value);
}
