package org.webbitserver;

import org.webbitserver.netty.NettyWebServer;

import java.net.SocketAddress;
import java.net.URI;

public class WebServers {

    /**
     * Returns a new {@link WebServer} object, which runs on the provided port.
     *
     * @param port
     * @return {@link WebServer} object
     * @see NettyWebServer
     */
    public static WebServer createWebServer(int port) {
        return new NettyWebServer(port);
    }

    /**
     * Returns a new {@link WebServer} object, adding the executor to the list
     * of executor services, running on the stated socket address and accessible
     * from the provided public URI.
     *
     *
     * @param socketAddress
     * @param publicUri
     * @return {@link WebServer} object
     * @see NettyWebServer
     */
    public static WebServer createWebServer(SocketAddress socketAddress, URI publicUri) {
        return new NettyWebServer(socketAddress, publicUri);
    }

}
