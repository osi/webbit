package org.webbitserver.helpers;

import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryParameters {
    private static final List<String> EMPTY = Collections.emptyList();
    private final Map<String, List<String>> params;

    public QueryParameters(String uri) {
        params = new QueryStringDecoder(uri).parameters();
    }

    public String first(String key) {
        List<String> all = all(key);
        return all.isEmpty() ? null : all.get(0);
    }

    public List<String> all(String key) {
        return params.containsKey(key) ? params.get(key) : EMPTY;
    }

    public Set<String> keys() {
        return params.keySet();
    }
}
