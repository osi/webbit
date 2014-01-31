package org.webbitserver.helpers;

import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryParameters {
    private final Map<String, List<String>> params;

    public QueryParameters(String uri, boolean hasPath) {
        params = new QueryStringDecoder(uri, hasPath).parameters();
    }

    public String first(String key) {
        List<String> all = all(key);
        return all.isEmpty() ? null : all.get(0);
    }

    public List<String> all(String key) {
        return params.containsKey(key) ? params.get(key) : Collections.<String>emptyList();
    }

    public Set<String> keys() {
        return params.keySet();
    }
}
