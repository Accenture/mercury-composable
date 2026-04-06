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
import org.platformlambda.core.serializers.SimpleXmlParser;
import org.platformlambda.core.serializers.SimpleXmlWriter;
import org.platformlambda.core.util.Utility;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpConverterXml extends AbstractHttpMessageConverter<Object> {
    private static final Utility util = Utility.getInstance();
    private static final SimpleXmlWriter map2xml = new SimpleXmlWriter();
    private static final SimpleXmlParser xml = new SimpleXmlParser();

    public HttpConverterXml() {
        super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return true;
    }

    @NonNull
    @Override
    public Object readInternal(@NonNull Class<?> clazz, HttpInputMessage inputMessage)
            throws HttpMessageNotReadableException {
        try {
            return xml.parse(inputMessage.getBody());
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        outputMessage.getHeaders().setContentType(MediaType.APPLICATION_XML);
        SimpleObjectMapper mapper = SimpleMapper.getInstance().getMapper();
        OutputStream out = outputMessage.getBody();
        switch (o) {
            case String text -> out.write(util.getUTF(text));
            case byte[] bytes -> out.write(bytes);
            default -> {
                final String root;
                final Map<String, Object> map;
                if (o instanceof List) {
                    root = "result";
                    map = new HashMap<>();
                    map.put("item", mapper.readValue(o, List.class));
                } else if (o instanceof Map) {
                    root = "result";
                    map = (Map<String, Object>) o;
                } else {
                    root = o.getClass().getSimpleName().toLowerCase();
                    map = mapper.readValue(o, Map.class);
                }
                String result = map2xml.write(root, map);
                out.write(util.getUTF(result));
            }
        }
    }
}
