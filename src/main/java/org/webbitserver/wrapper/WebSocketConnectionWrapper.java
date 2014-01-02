package org.webbitserver.wrapper;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.webbitserver.HttpRequest;
import org.webbitserver.WebSocketConnection;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public class WebSocketConnectionWrapper implements WebSocketConnection {

    private WebSocketConnection connection;

    public WebSocketConnectionWrapper(WebSocketConnection connection) {
        this.connection = connection;
    }

    public WebSocketConnection underlyingControl() {
        return connection;
    }

    public WebSocketConnectionWrapper underlyingControl(WebSocketConnection control) {
        this.connection = control;
        return this;
    }

    public WebSocketConnection originalControl() {
        if (connection instanceof WebSocketConnectionWrapper) {
            WebSocketConnectionWrapper wrapper = (WebSocketConnectionWrapper) connection;
            return wrapper.originalControl();
        } else {
            return connection;
        }
    }

    @Override
    public HttpRequest httpRequest() {
        return connection.httpRequest();
    }

    @Override
    public WebSocketConnectionWrapper send(TextWebSocketFrame frame) {
        connection.send(frame);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper send(BinaryWebSocketFrame frame) {
        connection.send(frame);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper ping(PingWebSocketFrame frame) {
        connection.ping(frame);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper pong(PongWebSocketFrame frame) {
        connection.pong(frame);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper send(String message) {
        connection.send(message);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper send(byte[] message) {
        connection.send(message);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper ping(byte[] msg) {
        connection.ping(msg);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper send(byte[] message, int offset, int length) {
        connection.send(message, offset, length);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper pong(byte[] msg) {
        connection.pong(msg);
        return this;
    }

    @Override
    public WebSocketConnectionWrapper close() {
        connection.close();
        return this;
    }

    @Override
    public WebSocketConnectionWrapper close(int status, String reason) {
        connection.close(status, reason);
        return this;
    }

    @Override
    public Map<String, Object> data() {
        return connection.data();
    }

    @Override
    public Object data(String key) {
        return connection.data(key);
    }

    @Override
    public WebSocketConnectionWrapper data(String key, Object value) {
        connection.data(key, value);
        return this;
    }

    @Override
    public Set<String> dataKeys() {
        return connection.dataKeys();
    }

    @Override
    public Executor handlerExecutor() {
        return connection.handlerExecutor();
    }

    @Override
    public String version() {
        return connection.version();
    }

    @Override
    public void execute(Runnable command) {
        connection.execute(command);
    }

}
