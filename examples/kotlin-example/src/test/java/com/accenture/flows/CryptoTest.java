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

package com.accenture.flows;

import com.accenture.support.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class CryptoTest extends TestBase {

    @SuppressWarnings("unchecked")
    @Test
    void encryptAndDecryptTest() throws ExecutionException, InterruptedException {
        byte[] key = crypto.generateAesKey(strongCrypto? 256 : 128);
        PostOffice po = new PostOffice("unit.test", "1000", "TEST /crypto");
        String key1 = "k1";
        String key2 = "k2";
        String key1Data = "hello";
        String key2Data = "world";
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> dataset = new HashMap<>();
        input.put("protected_fields", "k1, k2");
        input.put("key", key);
        dataset.put(key1, key1Data);
        dataset.put(key2, key2Data);
        input.put("dataset", dataset);
        // send event to encryption function
        EventEnvelope encRequest = new EventEnvelope().setTo("v1.encrypt.fields").setBody(input);
        EventEnvelope encResult = po.request(encRequest, 5000).get();
        assertInstanceOf(Map.class, encResult.getBody());
        Map<String, Object> encrypted = (Map<String, Object>) encResult.getBody();
        assertEquals(2, encrypted.size());
        assertTrue(encrypted.containsKey(key1));
        assertTrue(encrypted.containsKey(key2));
        assertNotEquals(key1Data, encrypted.get(key1));
        assertNotEquals(key2Data, encrypted.get(key2));
        // update encrypted dataset
        input.put("dataset", encrypted);
        // send event to decryption function
        EventEnvelope decRequest = new EventEnvelope().setTo("v1.decrypt.fields").setBody(input);
        EventEnvelope decResult = po.request(decRequest, 5000).get();
        assertInstanceOf(Map.class, decResult.getBody());
        Map<String, Object> decrypted = (Map<String, Object>) decResult.getBody();
        assertEquals(2, decrypted.size());
        assertTrue(decrypted.containsKey(key1));
        assertTrue(decrypted.containsKey(key2));
        assertEquals(key1Data, decrypted.get(key1));
        assertEquals(key2Data, decrypted.get(key2));
    }
}
