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

@PreLoad(route="v1.decrypt.fields", instances=100)
public class DecryptFields implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final Utility util = Utility.getInstance();
    private static final CryptoApi crypto = new CryptoApi();

    private static final String B64_MASTER_KEY = "b64_key";
    private static final String PROTECTED_FIELDS = "protected_fields";
    private static final String DATASET = "dataset";
    private static final String MISSING = "Missing ";

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws GeneralSecurityException, IOException {

        if (!input.containsKey(PROTECTED_FIELDS)) {
            throw new IllegalArgumentException(MISSING+PROTECTED_FIELDS);
        }
        if (!input.containsKey(B64_MASTER_KEY)) {
            throw new IllegalArgumentException(MISSING+ B64_MASTER_KEY);
        }
        if (input.containsKey(DATASET)) {
            String masterKey = input.get(B64_MASTER_KEY).toString();
            Map<String, Object> dataset = (Map<String, Object>) input.get(DATASET);
            MultiLevelMap multiLevels = new MultiLevelMap(dataset);
            List<String> fields = util.split((String) input.get(PROTECTED_FIELDS), ", ");
            for (String f: fields) {
                if (multiLevels.exists(f)) {
                    String cipherText = multiLevels.getElement(f).toString();
                    multiLevels.setElement(f, decryptField(cipherText, masterKey));
                }
            }
            return multiLevels.getMap();
        } else {
            throw new IllegalArgumentException(MISSING+DATASET);
        }
    }

    private String decryptField(String cipherText, String masterKey) throws GeneralSecurityException, IOException {
        byte[] encrypted = util.base64ToBytes(cipherText);
        byte[] b = crypto.aesDecrypt(encrypted, util.base64ToBytes(masterKey));
        return util.getUTF(b);
    }

}
