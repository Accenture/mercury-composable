/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package com.accenture.dictionary.models;

import java.net.URI;
import java.net.URISyntaxException;

public class HostAndUri {
    public final String host;
    public final String uri;

    public HostAndUri(String url) throws URISyntaxException {
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
