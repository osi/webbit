package org.webbitserver.netty;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.webbitserver.EventSourceHandler;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;

public class EventSourceConnectionHandler extends ChannelHandlerAdapter {
    private final ConnectionHelper connectionHelper;

    public EventSourceConnectionHandler(
            Executor executor,
            UncaughtExceptionHandler exceptionHandler,
            UncaughtExceptionHandler ioExceptionHandler,
            final NettyEventSourceConnection eventSourceConnection,
            final EventSourceHandler eventSourceHandler
    )
    {
        this.connectionHelper = new ConnectionHelper(executor, exceptionHandler, ioExceptionHandler) {
            @Override
            protected void fireOnClose() throws Exception {
                eventSourceHandler.onClose(eventSourceConnection);
            }
        };
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionHelper.fireOnClose(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        connectionHelper.fireConnectionException(ctx.channel(), cause);
    }

}
