package samples.authentication;

import org.webbitserver.WebServer;
import org.webbitserver.handler.StaticFileHandler;
import org.webbitserver.handler.authentication.BasicAuthenticationHandler;
import org.webbitserver.handler.authentication.InMemoryPasswords;

import static org.webbitserver.WebServers.createWebServer;

/**
 * This example demonstrates restricting access using HTTP BASIC authentication.
 * <p/>
 * Passwords are known in advance and stored in memory.
 */
public class SimplePasswordsExample {

    public static void main(String[] args) throws Exception {
        InMemoryPasswords passwords = new InMemoryPasswords()
                .add("joe", "secret")
                .add("jeff", "somepassword");

        WebServer webServer = createWebServer(45453)
                .add(new BasicAuthenticationHandler(passwords))
                .add("/whoami", new WhoAmIHttpHandler())
                .add("/whoami-ws", new WhoAmIWebSocketHandler())
                .add(new StaticFileHandler("src/test/java/samples/authentication/content"));

        webServer.start();

        System.out.println("Running on " + webServer.getUri());
    }

}
