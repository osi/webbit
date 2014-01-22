package org.webbitserver.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.webbitserver.EventSourceHandler;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.WebSocketHandler;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

public class NettyHttpControl implements HttpControl {
    private static final Pattern HTTPS = Pattern.compile("(?s)https://.*");

    private final Iterator<HttpHandler> handlerIterator;
    private final ChannelHandlerContext ctx;
    private final NettyHttpRequest webbitHttpRequest;
    private final io.netty.handler.codec.http.HttpResponse nettyHttpResponse;
    private final Thread.UncaughtExceptionHandler ioExceptionHandler;
    private final int maxWebSocketFrameSize;

    private HttpRequest defaultRequest;
    private HttpResponse webbitHttpResponse;
    private HttpControl defaultControl;
    private NettyWebSocketConnection webSocketConnection;
    private NettyEventSourceConnection eventSourceConnection;

    public NettyHttpControl(Iterator<HttpHandler> handlerIterator,
                            ChannelHandlerContext ctx,
                            NettyHttpRequest webbitHttpRequest,
                            NettyHttpResponse webbitHttpResponse,
                            io.netty.handler.codec.http.HttpResponse nettyHttpResponse,
                            Thread.UncaughtExceptionHandler ioExceptionHandler,
                            int maxWebSocketFrameSize)
    {
        this.handlerIterator = handlerIterator;
        this.ctx = ctx;
        this.webbitHttpRequest = webbitHttpRequest;
        this.webbitHttpResponse = webbitHttpResponse;
        this.nettyHttpResponse = nettyHttpResponse;
        this.ioExceptionHandler = ioExceptionHandler;
        this.maxWebSocketFrameSize = maxWebSocketFrameSize;

        defaultRequest = webbitHttpRequest;
        defaultControl = this;
    }

    @Override
    public void nextHandler() {
        nextHandler(defaultRequest, webbitHttpResponse, defaultControl);
    }

    @Override
    public void nextHandler(HttpRequest request, HttpResponse response) {
        nextHandler(request, response, defaultControl);
    }

    @Override
    public void nextHandler(HttpRequest request, HttpResponse response, HttpControl control) {
        this.defaultRequest = request;
        this.webbitHttpResponse = response;
        this.defaultControl = control;
        if (handlerIterator.hasNext()) {
            HttpHandler handler = handlerIterator.next();
            try {
                handler.handleHttpRequest(request, response, control);
            } catch (Throwable e) {
                response.error(e);
            }
        } else {
            response.status(404).end();
        }
    }

    @Override
    public WebSocketConnection upgradeToWebSocketConnection(final WebSocketHandler webSocketHandler) {
        if (null != webSocketConnection) {
            throw new IllegalStateException("already upgraded?");
        }

        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(getWebSocketLocation(), null, false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(webbitHttpRequest.netty());

        Channel channel = ctx.channel();
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(channel);
            // TODO this is bad..
            return webSocketConnection;
        }

        webSocketConnection =
                new NettyWebSocketConnection(
                        webbitHttpRequest,
                        ctx,
                        "Sec-WebSocket-Version-" + handshaker.version().toHttpHeaderValue());

        ChannelFuture handshakeComplete = handshaker.handshake(channel, webbitHttpRequest.netty());

        WebSocketConnectionHandler webSocketConnectionHandler = new WebSocketConnectionHandler(
                ioExceptionHandler,
                webSocketConnection,
                webSocketHandler,
                handshaker);
        channel.pipeline().replace("handler", "wshandler", webSocketConnectionHandler);
        channel.pipeline().addBefore("wshandler", "wsaggregator", new WebSocketFrameAggregator(maxWebSocketFrameSize));

        handshakeComplete.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            webSocketHandler.onOpen(webSocketConnection);
                        }
                    });
                }
            }
        });

        return webSocketConnection;
    }

    private String getWebSocketLocation() {
        return getWebSocketProtocol() + webbitHttpRequest.header(HttpHeaders.Names.HOST) + webbitHttpRequest.uri();
    }

    private String getWebSocketProtocol() {
        if (HTTPS.matcher(webbitHttpRequest.header(HttpHeaders.Names.ORIGIN)).matches()) {
            return "wss://";
        } else {
            return "ws://";
        }
    }

    @Override
    public NettyWebSocketConnection webSocketConnection() {
        return webSocketConnection;
    }

    @Override
    public NettyEventSourceConnection upgradeToEventSourceConnection(EventSourceHandler eventSourceHandler) {
        NettyEventSourceConnection eventSourceConnection = eventSourceConnection();
        EventSourceConnectionHandler eventSourceConnectionHandler = new EventSourceConnectionHandler(
                ioExceptionHandler,
                eventSourceConnection,
                eventSourceHandler);
        performEventSourceHandshake(eventSourceConnectionHandler);

        try {
            eventSourceHandler.onOpen(eventSourceConnection);
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
        return eventSourceConnection;
    }

    @Override
    public NettyEventSourceConnection eventSourceConnection() {
        if (eventSourceConnection == null) {
            eventSourceConnection = new NettyEventSourceConnection(webbitHttpRequest, ctx);
        }
        return eventSourceConnection;
    }

    @Override
    public Executor handlerExecutor() {
        return ctx.executor();
    }

    @Override
    public void execute(Runnable command) {
        ExecutorHelper.safeExecute(ctx, command);
    }

    private void performEventSourceHandshake(ChannelHandler eventSourceConnectionHandler) {
        nettyHttpResponse.setStatus(HttpResponseStatus.OK);
        HttpHeaders.setHeader(nettyHttpResponse, HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
        HttpHeaders.setHeader(nettyHttpResponse, HttpHeaders.Names.TRANSFER_ENCODING, "identity");
        HttpHeaders.setHeader(nettyHttpResponse, HttpHeaders.Names.CONNECTION, "keep-alive");
        HttpHeaders.setHeader(nettyHttpResponse, HttpHeaders.Names.CACHE_CONTROL, "no-cache");
        ctx.channel().write(nettyHttpResponse);
        ctx.channel().write(LastHttpContent.EMPTY_LAST_CONTENT);
        getReadyToSendEventSourceMessages(eventSourceConnectionHandler);
    }

    private void getReadyToSendEventSourceMessages(ChannelHandler eventSourceConnectionHandler) {
        Channel channel = ctx.channel();
        ChannelPipeline p = channel.pipeline();
        p.remove("aggregator");
        p.replace("handler", "ssehandler", eventSourceConnectionHandler);
    }
}
