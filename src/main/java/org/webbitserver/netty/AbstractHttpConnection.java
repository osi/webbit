package org.webbitserver.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.webbitserver.HttpConnection;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public abstract class AbstractHttpConnection implements HttpConnection {
    private final NettyHttpRequest nettyHttpRequest;
    protected final ChannelHandlerContext ctx;

    public AbstractHttpConnection(ChannelHandlerContext ctx, NettyHttpRequest nettyHttpRequest) {
        this.ctx = ctx;
        this.nettyHttpRequest = nettyHttpRequest;
    }

    protected ChannelFuture writeMessage(Object message) {
        ChannelFuture write = ctx.channel().write(message);
        write.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        return write;
    }

    protected void closeChannel() {
        ctx.channel().write(ctx.alloc().buffer(0, 0)).addListener(ChannelFutureListener.CLOSE);
    }

    protected void putData(String key, Object value) {
        data().put(key, value);
    }

    @Override
    public NettyHttpRequest httpRequest() {
        return nettyHttpRequest;
    }

    @Override
    public Map<String, Object> data() {
        return nettyHttpRequest.data();
    }

    @Override
    public Object data(String key) {
        return data().get(key);
    }

    @Override
    public Set<String> dataKeys() {
        return data().keySet();
    }

    @Override
    public Executor handlerExecutor() {
        return ctx.executor();
    }

    @Override
    public void execute(Runnable command) {
        ExecutorHelper.safeExecute(ctx, command);
    }
}
