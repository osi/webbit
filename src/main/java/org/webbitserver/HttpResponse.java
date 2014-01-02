package org.webbitserver;

import io.netty.handler.codec.http.Cookie;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Writes a response back to the client.
 * <p/>
 * IMPORTANT: The connection will remain open until {@link #end()} or {@link #error(Throwable)} is called. Don't
 * forget these!
 *
 * @author Joe Walnes
 */
public interface HttpResponse {
    /**
     * For text based responses, sets the Charset to encode the response as.
     * <p/>
     * If not set, defaults to UTF8.
     */
    HttpResponse charset(Charset charset);

    /**
     * Turns the response into a chunked response
     * <p/>
     * after this method is called, {@link #write(String)} should be used to send chunks
     * <p/>
     * TODO support just writing something Chunked as the content
     */
    HttpResponse chunked();

    /**
     * Current Charset used to encode to response as.
     *
     * @see #charset(Charset)
     */
    Charset charset();

    /**
     * Sets the HTTP status code.
     * <p/>
     * Defaults to 200 (OK).
     */
    HttpResponse status(int status);

    /**
     * Retrieve HTTP status code that this response is going to return.
     *
     * @see #status(int)
     */
    int status();

    /**
     * Adds an HTTP header. Multiple HTTP headers can be added with the same name.
     */
    HttpResponse header(CharSequence name, CharSequence value);

    /**
     * Adds a numeric HTTP header. Multiple HTTP headers can be added with the same name.
     */
    HttpResponse header(String name, long value);

    /**
     * Adds a Date (RFC 1123 format) HTTP header. Multiple HTTP headers can be added with the same name.
     */
    HttpResponse header(CharSequence name, Date value);

    /**
     * Test to see if this response has a header of the specified name
     *
     * @param name
     */
    boolean containsHeader(CharSequence name);

    /**
     * Adds a cookie
     *
     * @param cookie the cookie
     */
    HttpResponse cookie(Cookie cookie);

    /**
     * Write text based content back to the client.
     *
     * @see #charset(Charset)
     * @see #content(byte[])
     */
    HttpResponse content(String content);

    /**
     * Write binary based content back to the client.
     *
     * @see #content(String)
     */
    HttpResponse content(byte[] content);

    /**
     * Write binary based content back to the client.
     *
     * @see #content(String)
     */
    HttpResponse content(ByteBuffer buffer);

    /**
     * Marks the response as erroneous. The error shall be displayed to the user (500 SERVER ERROR)
     * and the connection closed.
     * <p/>
     * Every response should have either {@link #end()} or {@link #error(Throwable)} called. No more
     * operations should be performed on a response after these.
     */
    HttpResponse error(Throwable error);

    /**
     * Marks the response as ended. At this point any remaining data shall be flushed and
     * the connection closed.
     * <p/>
     * Every response should have either {@link #end()} or {@link #error(Throwable)} called. No more
     * operations should be performed on a response after these.
     */
    HttpResponse end();
}
