package org.webbitserver.netty;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.webbitserver.EventSourceHandler;
import org.webbitserver.WebbitException;

import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.channels.ClosedChannelException;

public class EventSourceConnectionHandler extends ChannelHandlerAdapter {
    private final UncaughtExceptionHandler ioExceptionHandler;
    private final NettyEventSourceConnection eventSourceConnection;
    private final EventSourceHandler eventSourceHandler;

    public EventSourceConnectionHandler(UncaughtExceptionHandler ioExceptionHandler,
                                        NettyEventSourceConnection eventSourceConnection,
                                        EventSourceHandler eventSourceHandler
    )
    {
        this.ioExceptionHandler = ioExceptionHandler;
        this.eventSourceConnection = eventSourceConnection;
        this.eventSourceHandler = eventSourceHandler;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        eventSourceHandler.onClose(eventSourceConnection);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getCause() instanceof ClosedChannelException) {
            ctx.close();
        } else {
            ioExceptionHandler.uncaughtException(Thread.currentThread(),
                                                 WebbitException.fromException(cause, ctx.channel()));
        }
    }

}
