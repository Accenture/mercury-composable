/*

    Copyright 2018-2025 Accenture Technology

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

package com.accenture.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@PreLoad(route="v1.encrypt.fields", instances=10)
public class EncryptFields implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final Utility util = Utility.getInstance();
    private static final CryptoApi crypto = new CryptoApi();
    private static final String KEY = "key";
    private static final String PROTECTED_FIELDS = "protected_fields";
    private static final String DATASET = "dataset";
    private static final String MISSING = "Missing ";

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws Exception {
        if (!input.containsKey(PROTECTED_FIELDS)) {
            throw new IllegalArgumentException(MISSING+PROTECTED_FIELDS);
        }
        if (!input.containsKey(KEY)) {
            throw new IllegalArgumentException(MISSING + KEY);
        }
        Object keyBytes = input.get(KEY);
        if (!(keyBytes instanceof byte[])) {
            throw new IllegalArgumentException(KEY + " - Expect bytes, Actual: " + keyBytes.getClass());
        }
        if (input.containsKey(DATASET)) {
            byte[] key = (byte[]) keyBytes;
            Map<String, Object> dataset = (Map<String, Object>) input.get(DATASET);
            MultiLevelMap multiLevels = new MultiLevelMap(dataset);
            List<String> fields = util.split((String) input.get(PROTECTED_FIELDS), ", ");
            for (String f: fields) {
                if (multiLevels.exists(f)) {
                    byte[] clearText = util.getUTF(String.valueOf(multiLevels.getElement(f)));
                    multiLevels.setElement(f, encryptField(clearText, key));
                }
            }
            return multiLevels.getMap();
        } else {
            throw new IllegalArgumentException(MISSING + DATASET);
        }
    }

    private String encryptField(byte[] clearText, byte[] key) throws GeneralSecurityException, IOException {
        byte[] b = crypto.aesEncrypt(clearText, key);
        return util.bytesToBase64(b);
    }
}
