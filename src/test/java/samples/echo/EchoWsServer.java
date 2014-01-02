package samples.echo;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebServer;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.handler.HttpToWebSocketHandler;
import org.webbitserver.handler.exceptions.PrintStackTraceExceptionHandler;

import java.io.IOException;
import java.net.URI;

public class EchoWsServer {

    private final WebServer webServer;

    public EchoWsServer(WebServer webServer) {
        this.webServer = webServer;
        webServer.add(new HttpToWebSocketHandler(new EchoHandler()))
                .connectionExceptionHandler(new PrintStackTraceExceptionHandler());
    }

    public void start() throws Exception {
        webServer.start();
    }

    public URI uri() throws IOException {
        return webServer.getUri();
    }

    public void stop() throws Exception {
        webServer.stop();
    }

    private static class EchoHandler extends BaseWebSocketHandler {
        @Override
        public void onMessage(WebSocketConnection connection, TextWebSocketFrame msg) throws Exception {
            connection.send(msg.retain());
        }

        @Override
        public void onMessage(WebSocketConnection connection, BinaryWebSocketFrame msg) {
            connection.send(msg.retain());
        }
    }
}
