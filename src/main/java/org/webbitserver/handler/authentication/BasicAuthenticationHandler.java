package org.webbitserver.handler.authentication;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Adds HTTP Basic authentication to a page. Users should provide an implementation of UsernamePasswordAuthenticator
 * to check the supplied credentials.
 * <p/>
 * See samples.authentication.SimplePasswordsExample in the src/tests directory for a really basic usage. To implement
 * a custom authenticator that performs background IO, see samples.authentication.AsyncPasswordsExample.
 *
 * @see PasswordAuthenticator
 * @see InMemoryPasswords
 */
public class BasicAuthenticationHandler implements HttpHandler {

    public static final String USERNAME = "user";

    private static final String BASIC_PREFIX = "Basic ";

    private final String realm;
    private final PasswordAuthenticator authenticator;

    public BasicAuthenticationHandler(PasswordAuthenticator authenticator) {
        this(authenticator, "Secure Area");
    }

    public BasicAuthenticationHandler(PasswordAuthenticator authenticator, String realm) {
        this.realm = realm;
        this.authenticator = authenticator;
    }

    @Override
    public void handleHttpRequest(final HttpRequest request, final HttpResponse response, final HttpControl control)
            throws Exception
    {
        String authHeader = request.header("Authorization");
        if (authHeader == null) {
            needAuthentication(response);
        } else {
            if (authHeader.startsWith(BASIC_PREFIX)) {
                ByteBuf byteBuf =
                        Base64.decode(Unpooled.copiedBuffer(authHeader.substring(BASIC_PREFIX.length()), US_ASCII));
                String decoded = byteBuf.toString(US_ASCII);
                String[] pair = decoded.split(":", 2);
                if (pair.length == 2) {
                    final String username = pair[0];
                    String password = pair[1];
                    PasswordAuthenticator.ResultCallback callback = new PasswordAuthenticator.ResultCallback() {
                        @Override
                        public void success() {
                            request.data(USERNAME, username);
                            control.nextHandler();
                        }

                        @Override
                        public void failure() {
                            needAuthentication(response);
                        }
                    };

                    authenticator.authenticate(request, username, password, callback, control);
                } else {
                    needAuthentication(response);
                }
            }
        }
    }

    private void needAuthentication(HttpResponse response) {
        response.status(401)
                .header("WWW-Authenticate", "Basic realm=\"" + realm + "\"")
                .content("Need authentication")
                .end();
    }

}
