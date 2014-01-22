package org.webbitserver.handler.logging;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.WebSocketHandler;

class LoggingWebSocketHandler implements WebSocketHandler {

    private final LogSink logSink;
    private final WebSocketConnection loggingConnection;
    private final WebSocketHandler handler;

    LoggingWebSocketHandler(LogSink logSink, WebSocketConnection loggingConnection, WebSocketHandler handler) {
        this.logSink = logSink;
        this.loggingConnection = loggingConnection;
        this.handler = handler;
    }

    @Override
    public void onOpen(WebSocketConnection connection) {
        logSink.webSocketConnectionOpen(connection);
        handler.onOpen(loggingConnection);
    }

    @Override
    public void onClose(WebSocketConnection connection) throws Exception {
        logSink.webSocketConnectionClose(connection);
        logSink.httpEnd(connection.httpRequest());
        handler.onClose(loggingConnection);
    }

    @Override
    public void onMessage(WebSocketConnection connection, TextWebSocketFrame message) throws Exception {
        logSink.webSocketInboundData(connection, message.text());
        handler.onMessage(loggingConnection, message);
    }

    @Override
    public void onMessage(WebSocketConnection connection, BinaryWebSocketFrame message) throws Exception {
        logSink.webSocketInboundData(connection, message.retain());
        handler.onMessage(loggingConnection, message);
    }

    @Override
    public void onPing(WebSocketConnection connection, PingWebSocketFrame message) throws Exception {
        logSink.webSocketInboundPing(connection, message.retain());
        handler.onPing(loggingConnection, message);
    }

    @Override
    public void onPong(WebSocketConnection connection, PongWebSocketFrame message) throws Exception {
        logSink.webSocketInboundPong(connection, message.retain());
        handler.onPong(loggingConnection, message);
    }
}
