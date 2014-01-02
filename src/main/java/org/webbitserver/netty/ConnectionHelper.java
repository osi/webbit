package org.webbitserver.netty;

import io.netty.channel.Channel;
import org.webbitserver.WebbitException;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Executor;

abstract class ConnectionHelper {
    protected final Executor executor;
    protected final Thread.UncaughtExceptionHandler exceptionHandler;
    private final Thread.UncaughtExceptionHandler ioExceptionHandler;

    public ConnectionHelper(Executor executor,
                            Thread.UncaughtExceptionHandler exceptionHandler,
                            Thread.UncaughtExceptionHandler ioExceptionHandler)
    {
        this.ioExceptionHandler = ioExceptionHandler;
        this.executor = executor;
        this.exceptionHandler = exceptionHandler;
    }

    public void fireOnClose(Channel channel) {
        final Thread thread = Thread.currentThread();
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                webbitExceptionWrappingExceptionHandler(channel);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    fireOnClose();
                } catch (Throwable t) {
                    uncaughtExceptionHandler.uncaughtException(thread, t);
                }
            }
        });
    }

    public void fireConnectionException(final Channel channel, final Throwable cause) {
        if (cause.getCause() instanceof ClosedChannelException) {
            channel.close();
        } else {
            final Thread thread = Thread.currentThread();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    ioExceptionHandler.uncaughtException(thread, WebbitException.fromException(cause, channel));
                }
            });
        }
    }

    protected abstract void fireOnClose() throws Throwable;

    // Uncaught exception handler including the connection for context.
    protected Thread.UncaughtExceptionHandler webbitExceptionWrappingExceptionHandler(final Channel channel) {
        return new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                exceptionHandler.uncaughtException(t, WebbitException.fromException(e, channel));
            }
        };
    }
}
