package org.webbitserver.helpers;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class QueryParametersTest {
    @Test
    public void parsesOneParameter() throws Exception {
        assertEquals("bar", new QueryParameters("foo=bar", false).first("foo"));
    }

    @Test
    public void parsesTwoParameters() throws Exception {
        assertEquals(asList("b", "e"), new QueryParameters("a=b&c=d&a=e", false).all("a"));
    }

    @Test
    public void parsesEmptyParameter() throws Exception {
        assertEquals("", new QueryParameters("a=", false).first("a"));
    }

    @Test
    public void parsesMixOfPresentAndEmptyParameters() throws Exception {
        assertEquals(asList("b", "", "e"), new QueryParameters("a=b&a=&a=e", false).all("a"));

    }
}
