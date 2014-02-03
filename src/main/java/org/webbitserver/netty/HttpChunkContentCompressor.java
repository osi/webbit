package org.webbitserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;

public class HttpChunkContentCompressor extends HttpContentCompressor {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            super.write(ctx, new DefaultHttpContent((ByteBuf) msg), promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
