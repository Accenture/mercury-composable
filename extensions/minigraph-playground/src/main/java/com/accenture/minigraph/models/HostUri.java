package com.accenture.minigraph.models;

import java.net.URI;
import java.net.URISyntaxException;

public class HostUri {
    public final String host;
    public final String uri;

    public HostUri(String url) throws URISyntaxException {
        // avoid URI syntax error by hiding path parameter brackets and spaces
        var safeUrl = url.replace('{', '(').replace('}', ')')
                .replace(' ', '+');
        var hostPath = new URI(safeUrl);
        var path = hostPath.getPath();
        var sep = path.isEmpty() ? -1 : safeUrl.lastIndexOf(path);
        this.host = sep == -1 ? url : url.substring(0, sep);
        this.uri = sep == -1 ? "/" : url.substring(sep);
    }
}
