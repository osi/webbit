package org.webbitserver;

import java.util.concurrent.Executor;

public interface HttpControl extends Executor {

    void nextHandler();

    void nextHandler(HttpRequest request, HttpResponse response);

    void nextHandler(HttpRequest request, HttpResponse response, HttpControl control);

    WebSocketConnection upgradeToWebSocketConnection(WebSocketHandler handler);

    /**
     * Returns the WebSockedConnection under control, but only after upgrading. Otherwise returns null.
     *
     * @return
     */
    WebSocketConnection webSocketConnection();

    EventSourceConnection upgradeToEventSourceConnection(EventSourceHandler handler);

    EventSourceConnection eventSourceConnection();

    Executor handlerExecutor();
}
