/*

    Copyright 2018-2024 Accenture Technology

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

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@PreLoad(route="v1.decrypt.fields", instances=100)
public class DecryptFields implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final Utility util = Utility.getInstance();
    private static final CryptoApi crypto = new CryptoApi();

    private static final String PROTECTED_FIELDS = "protected_fields";
    private static final String DATASET = "dataset";
    private static final String MISSING = "Missing ";
    private static final String TEMP_KEY_STORE = "/tmp/keystore";
    private static final String DEMO_MASTER_KEY = "demo.txt";

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws GeneralSecurityException, IOException {

        if (!input.containsKey(PROTECTED_FIELDS)) {
            throw new IllegalArgumentException(MISSING+PROTECTED_FIELDS);
        }
        if (input.containsKey(DATASET)) {
            Map<String, Object> dataset = (Map<String, Object>) input.get(DATASET);
            MultiLevelMap map = new MultiLevelMap(dataset);
            List<String> fields = util.split((String) input.get(PROTECTED_FIELDS), ", ");
            for (String f: fields) {
                if (map.exists(f)) {
                    String cipherText = map.getElement(f).toString();
                    map.setElement(f, decryptField(cipherText));
                }
            }
            return dataset;
        } else {
            throw new IllegalArgumentException(MISSING+DATASET);
        }
    }

    private String decryptField(String cipherText) throws GeneralSecurityException, IOException {
        File folder = new File(TEMP_KEY_STORE);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File f = new File(folder, DEMO_MASTER_KEY);
        if (!f.exists()) {
            String b64Key = util.bytesToBase64(crypto.generateAesKey(256));
            util.str2file(f, b64Key);
        }
        byte[] key = util.base64ToBytes(util.file2str(f));
        byte[] encrypted = util.base64ToBytes(cipherText);
        byte[] b = crypto.aesDecrypt(encrypted, key);
        return util.getUTF(b);
    }

}
