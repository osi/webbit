package org.webbitserver.netty;

import io.netty.channel.ChannelHandlerContext;

public class ExecutorHelper {
    public static void safeExecute(final ChannelHandlerContext ctx, final Runnable command) {
        if (ctx.executor().inEventLoop()) {
            command.run();
        } else {
            ctx.executor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.run();
                    } catch (Exception e) {
                        ctx.fireExceptionCaught(e);
                    }
                }
            });
        }
    }
}
