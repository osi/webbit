package org.webbitserver.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import org.webbitserver.EventSourceConnection;
import org.webbitserver.EventSourceMessage;

import java.util.concurrent.Executor;


public class NettyEventSourceConnection extends AbstractHttpConnection implements EventSourceConnection {
    public NettyEventSourceConnection(Executor executor, NettyHttpRequest nettyHttpRequest, ChannelHandlerContext ctx) {
        super(ctx, nettyHttpRequest, executor);
    }

    @Override
    public NettyEventSourceConnection send(EventSourceMessage message) {
        writeMessage(Unpooled.copiedBuffer(message.build(), CharsetUtil.UTF_8));
        return this;
    }

    @Override
    public NettyEventSourceConnection data(String key, Object value) {
        putData(key, value);
        return this;
    }

    @Override
    public NettyEventSourceConnection close() {
        closeChannel();
        return this;
    }
}
