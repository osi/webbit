package org.webbitserver.handler.logging;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.webbitserver.EventSourceConnection;
import org.webbitserver.HttpRequest;
import org.webbitserver.WebSocketConnection;

public interface LogSink {

    void httpStart(HttpRequest request);

    void httpEnd(HttpRequest request);

    void webSocketConnectionOpen(WebSocketConnection connection);

    void webSocketConnectionClose(WebSocketConnection connection);

    void webSocketInboundData(WebSocketConnection connection, String data);

    void webSocketInboundData(WebSocketConnection connection, BinaryWebSocketFrame message);

    void webSocketInboundPing(WebSocketConnection connection, PingWebSocketFrame msg);

    void webSocketInboundPong(WebSocketConnection connection, PongWebSocketFrame msg);

    void webSocketOutboundData(WebSocketConnection connection, String data);

    void webSocketOutboundData(WebSocketConnection connection, byte[] data);

    void webSocketOutboundPing(WebSocketConnection connection, byte[] msg);

    void webSocketOutboundPong(WebSocketConnection connection, byte[] msg);

    void error(HttpRequest request, Throwable error);

    void custom(HttpRequest request, String action, String data);

    void eventSourceConnectionOpen(EventSourceConnection connection);

    void eventSourceConnectionClose(EventSourceConnection connection);

    void eventSourceOutboundData(EventSourceConnection connection, String data);
}
