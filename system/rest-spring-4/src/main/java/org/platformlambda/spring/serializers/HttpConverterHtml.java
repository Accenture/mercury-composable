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

package org.platformlambda.spring.serializers;

import org.jspecify.annotations.NonNull;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleObjectMapper;
import org.platformlambda.core.util.Utility;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpConverterHtml extends AbstractHttpMessageConverter<Object> {
    private static final Utility util = Utility.getInstance();
    private static final MediaType HTML_CONTENT = new MediaType("text", "html", StandardCharsets.UTF_8);

    public HttpConverterHtml() {
        super(HTML_CONTENT);
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return true;
    }

    @NonNull
    @Override
    public Object readInternal(@NonNull Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return util.getUTF(util.stream2bytes(inputMessage.getBody(), false));
    }

    @Override
    public void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        outputMessage.getHeaders().setContentType(HTML_CONTENT);
        SimpleObjectMapper mapper = SimpleMapper.getInstance().getMapper();
        OutputStream out = outputMessage.getBody();
        switch (o) {
            case String text -> out.write(util.getUTF(text));
            case byte[] bytes -> out.write(bytes);
            default -> {
                out.write(util.getUTF("<html><body><pre>\n"));
                out.write(mapper.writeValueAsBytes(o));
                out.write(util.getUTF("\n</pre></body></html>"));
            }
        }
    }
}
