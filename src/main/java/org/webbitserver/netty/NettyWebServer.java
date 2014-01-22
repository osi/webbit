package org.webbitserver.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import org.webbitserver.EventSourceHandler;
import org.webbitserver.HttpHandler;
import org.webbitserver.WebServer;
import org.webbitserver.WebSocketHandler;
import org.webbitserver.WebbitException;
import org.webbitserver.handler.DateHeaderHandler;
import org.webbitserver.handler.HttpToEventSourceHandler;
import org.webbitserver.handler.HttpToWebSocketHandler;
import org.webbitserver.handler.PathMatchHandler;
import org.webbitserver.handler.ServerHeaderHandler;
import org.webbitserver.handler.exceptions.PrintStackTraceExceptionHandler;
import org.webbitserver.handler.exceptions.SilentExceptionHandler;
import org.webbitserver.helpers.SslFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class NettyWebServer implements WebServer {
    private final SocketAddress socketAddress;
    private final URI publicUri;
    private final List<HttpHandler> handlers = new ArrayList<>();

    private Channel channel;
    private SSLContext sslContext;

    private EventLoopGroup ioBossGroup;
    private EventLoopGroup ioWorkerGroup;
    private EventExecutorGroup httpHandlerGroup;

    private Thread.UncaughtExceptionHandler exceptionHandler;
    private Thread.UncaughtExceptionHandler ioExceptionHandler;
    private int maxInitialLineLength = 4096;
    private int maxHeaderSize = 8192;
    private int maxChunkSize = 8192;
    private int maxContentLength = 65536;
    private int maxWebSocketFrameSize = 8192;

    public NettyWebServer(int port) {
        this(new InetSocketAddress(port), localUri(port));
    }

    public NettyWebServer(SocketAddress socketAddress, URI publicUri) {
        this.socketAddress = socketAddress;
        this.publicUri = publicUri;

        // Uncaught exceptions from handlers get dumped to console by default.
        // To change, call uncaughtExceptionHandler()
        uncaughtExceptionHandler(new PrintStackTraceExceptionHandler());

        // Default behavior is to silently discard any exceptions caused
        // when reading/writing to the client. The Internet is flaky - it happens.
        connectionExceptionHandler(new SilentExceptionHandler());

        setupDefaultHandlers();
    }

    protected void setupDefaultHandlers() {
        add(new ServerHeaderHandler("Webbit2"));
        add(new DateHeaderHandler());
    }

    @Override
    public NettyWebServer setupSsl(InputStream keyStore, String pass) throws WebbitException {
        return setupSsl(keyStore, pass, pass);
    }

    @Override
    public NettyWebServer setupSsl(InputStream keyStore, String storePass, String keyPass) throws WebbitException {
        this.sslContext = new SslFactory(keyStore, storePass).getServerContext(keyPass);
        return this;
    }

    @Override
    public URI getUri() {
        return publicUri;
    }

    @Override
    public int getPort() {
        if (publicUri.getPort() == -1) {
            return "https".equalsIgnoreCase(publicUri.getScheme()) ? 443 : 80;
        }
        return publicUri.getPort();
    }

    @Override
    public Executor getExecutor() {
        return httpHandlerGroup;
    }

    @Override
    public NettyWebServer staleConnectionTimeout(long millis) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public NettyWebServer add(HttpHandler handler) {
        handlers.add(handler);
        return this;
    }

    @Override
    public NettyWebServer add(String path, HttpHandler handler) {
        return add(new PathMatchHandler(path, handler));
    }

    @Override
    public NettyWebServer add(String path, WebSocketHandler handler) {
        return add(path, new HttpToWebSocketHandler(handler));
    }

    @Override
    public NettyWebServer add(String path, EventSourceHandler handler) {
        return add(path, new HttpToEventSourceHandler(handler));
    }

    @Override
    public void start() throws Exception {
        if (isRunning()) {
            throw new IllegalStateException("Server already started.");
        }

        ioBossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(getClass().getSimpleName() + ".acceptor"));
        ioWorkerGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(getClass().getSimpleName() + ".worker"));
        httpHandlerGroup = new DefaultEventLoop(new DefaultThreadFactory(getClass().getSimpleName() + ".httpHandler"));

        // Configure the server.
        channel = new ServerBootstrap()
                .group(ioBossGroup, ioWorkerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new NettyWebServerInitializer())
                .bind(socketAddress)
                .sync()
                .channel();
    }

    public boolean isRunning() {
        return channel != null && channel.isActive();
    }

    @Override
    public void stop() throws Exception {
        try {
            if (null != channel) {
                channel.close().sync();
            }
        } finally {
            List<Future<?>> futures = new ArrayList<>(3);

            if (null != ioBossGroup) {
                futures.add(ioBossGroup.shutdownGracefully());
            }
            if (null != ioWorkerGroup) {
                futures.add(ioWorkerGroup.shutdownGracefully());
            }
            if (null != httpHandlerGroup) {
                futures.add(httpHandlerGroup.shutdownGracefully());
            }

            for (Future<?> future : futures) {
                future.sync();
            }
        }
    }

    @Override
    public NettyWebServer uncaughtExceptionHandler(Thread.UncaughtExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    @Override
    public NettyWebServer connectionExceptionHandler(Thread.UncaughtExceptionHandler ioExceptionHandler) {
        this.ioExceptionHandler = ioExceptionHandler;
        return this;
    }

    /**
     * @see HttpRequestDecoder
     */
    public NettyWebServer maxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        return this;
    }

    /**
     * @see HttpObjectAggregator
     */
    public NettyWebServer maxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
        return this;
    }

    /**
     * @see HttpRequestDecoder
     */
    public NettyWebServer maxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
        return this;
    }

    /**
     * @see HttpRequestDecoder
     */
    public NettyWebServer maxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
        return this;
    }

    public NettyWebServer maxWebSocketFrameSize(int maxWebSocketFrameSize) {
        this.maxWebSocketFrameSize = maxWebSocketFrameSize;
        return this;
    }

    private static URI localUri(int port) {
        try {
            return URI.create("http://" + InetAddress.getLocalHost()
                    .getHostName() + (port == 80 ? "" : (":" + port)) + "/");
        } catch (UnknownHostException e) {
            throw new RuntimeException(
                    "can not create URI from localhost hostname - use constructor to pass an explicit URI",
                    e);
        }
    }

    protected long timestamp() {
        return System.currentTimeMillis();
    }

    class NettyWebServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();

            long timestamp = timestamp();
            if (sslContext != null) {
                SSLEngine engine = sslContext.createSSLEngine();
                engine.setUseClientMode(false);
                pipeline.addLast("ssl", new SslHandler(engine));
            }
//  TODO          pipeline.addLast("connectiontracker", connectionTrackingHandler);

            pipeline.addLast("codec-http", new HttpServerCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize));
            pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
            // cannot use compression and chunked writing by default together
            // http://stackoverflow.com/questions/20136334/netty-httpstaticfileserver-example-not-working-with-httpcontentcompressor
//            pipeline.addLast("decompressor", new HttpContentDecompressor());
//            pipeline.addLast("compressor", new HttpContentCompressor());
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

            // TODO for stale, look at the netty idle stuff

            pipeline.addLast(httpHandlerGroup,
                             "handler",
                             new NettyHttpChannelHandler(
                                     handlers,
                                     timestamp,
                                     exceptionHandler,
                                     ioExceptionHandler,
                                     maxWebSocketFrameSize));
        }
    }
}
