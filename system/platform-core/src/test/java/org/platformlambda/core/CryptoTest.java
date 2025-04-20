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

package org.platformlambda.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

class CryptoTest {
    private static final Logger log = LoggerFactory.getLogger(CryptoTest.class);

    private static final CryptoApi crypto = new CryptoApi();
    private static boolean strongCrypto;

    @BeforeAll
    public static void checkCrypto() {
        strongCrypto = crypto.strongCryptoSupported();
        if (!strongCrypto) {
            log.warn("Not using Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy");
            log.info("AES-128 supported");
        } else {
            log.info("Using Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy");
            log.info("AES-256 supported");
        }
    }

    @Test
    void aesEncryptionTest() throws IOException, GeneralSecurityException {
        String input = "hello world";
        byte[] key = crypto.generateAesKey(strongCrypto? 256 : 128);
        byte[] encrypted = crypto.aesEncrypt(input.getBytes(), key);
        byte[] decrypted = crypto.aesDecrypt(encrypted, key);
        assertEquals(input, new String(decrypted));
        // streaming methods
        ByteArrayInputStream clearIn = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
        crypto.aesEncrypt(clearIn, encryptedOut, key);
        encrypted = encryptedOut.toByteArray();
        ByteArrayInputStream encryptedIn = new ByteArrayInputStream(encrypted);
        ByteArrayOutputStream clearOut = new ByteArrayOutputStream();
        crypto.aesDecrypt(encryptedIn, clearOut, key);
        decrypted = clearOut.toByteArray();
        assertEquals(input, new String(decrypted));
    }

    @Test
    void rsaEncryptionTest() throws GeneralSecurityException {
        // RSA encryption is usually used to transport symmetric encryption key
        byte[] input = crypto.generateAesKey(256);
        KeyPair kp = crypto.generateRsaKey();
        byte[] pub = kp.getPublic().getEncoded();
        byte[] pri = kp.getPrivate().getEncoded();
        // encrypt
        byte[] encrypted = crypto.rsaEncrypt(input, pub);
        // decrypt
        byte[] decrypted = crypto.rsaDecrypt(encrypted, pri);
        // do a byte-by-byte comparison
        assertArrayEquals(input, decrypted);
    }

    @Test
    void invalidRsaKeyLength() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                            () -> crypto.generateRsaKey(1000));
        assertEquals("Key size must be one of [2048, 3072, 4096]", ex.getMessage());
    }

    @Test
    void pemReadWriteTest() {
        KeyPair kp = crypto.generateRsaKey();
        byte[] pub = kp.getPublic().getEncoded();
        String pem = crypto.writePem(pub, "PUBLIC KEY");
        byte[] restored = crypto.readPem(pem);
        String pemRestored = crypto.writePem(restored, "PUBLIC KEY");
        assertEquals(pem, pemRestored);
    }

    @Test
    void randomIntegerTest() {
        int n1 = crypto.nextInt(10000);
        int n2 = crypto.nextInt(10000);
        assertNotEquals(n1, n2);
    }

    @Test
    void publicPrivateKeyEncodingTest() throws GeneralSecurityException {
        KeyPair kp = crypto.generateRsaKey();
        byte[] pub = kp.getPublic().getEncoded();
        byte[] pri = kp.getPrivate().getEncoded();
        PublicKey publicKey = crypto.getPublic(pub);
        PrivateKey privateKey = crypto.getPrivate(pri);
        assertArrayEquals(pub, publicKey.getEncoded());
        assertArrayEquals(pri, privateKey.getEncoded());
        byte[] pubAgain = crypto.getEncodedPublicKey(kp);
        byte[] priAgain = crypto.getEncodedPrivateKey(kp);
        assertArrayEquals(pubAgain, publicKey.getEncoded());
        assertArrayEquals(priAgain, privateKey.getEncoded());
    }

    @Test
    void dsaSignatureTest() throws GeneralSecurityException {
        KeyPair kp = crypto.generateDsaKey();
        byte[] pub = kp.getPublic().getEncoded();
        byte[] pri = kp.getPrivate().getEncoded();
        byte[] data = "hello world".getBytes();
        byte[] signature = crypto.dsaSign(data, pri);
        boolean result = crypto.dsaVerify(data, signature, pub);
        assertTrue(result);
    }

    @Test
    void rsaSignatureTest() throws GeneralSecurityException {
        KeyPair kp = crypto.generateRsaKey();
        byte[] pub = kp.getPublic().getEncoded();
        byte[] pri = kp.getPrivate().getEncoded();
        byte[] data = "hello world".getBytes();
        byte[] signature = crypto.rsaSign(data, pri);
        boolean result = crypto.rsaVerify(data, signature, pub);
        assertTrue(result);
    }

    @Test
    void hashTest() throws IOException {
        String input = "hello world";
        byte[] hashed = crypto.getSHA256(input.getBytes());
        assertEquals(32, hashed.length);
        byte[] hashedFromStream = crypto.getSHA256(new ByteArrayInputStream(input.getBytes()));
        assertArrayEquals(hashed, hashedFromStream);
        hashed = crypto.getSHA512(input.getBytes());
        assertEquals(64, hashed.length);
        hashedFromStream = crypto.getSHA512(new ByteArrayInputStream(input.getBytes()));
        assertArrayEquals(hashed, hashedFromStream);
        hashed = crypto.getSHA256(input.getBytes());
        assertEquals(32, hashed.length);
        hashedFromStream = crypto.getSHA256(new ByteArrayInputStream(input.getBytes()));
        assertArrayEquals(hashed, hashedFromStream);
    }

    @Test
    void hmac256Test() {
        String expected = "f1ac9702eb5faf23ca291a4dc46deddeee2a78ccdaf0a412bed7714cfffb1cc4";
        byte[] key = "hello".getBytes();
        byte[] message = "world".getBytes();
        byte[] b = crypto.getHmacSha256(key, message);
        assertEquals(expected, Utility.getInstance().bytes2hex(b));
    }

    @Test
    void hmac512Test() {
        String expected = "6668ed2f7d016c5f12d7808fc4f2d1dc4851622d7f15616de947a823b3ee67d76" +
                            "1b953f09560da301f832902020dd1c64f496df37eb7ac4fd2feeeb67d77ba9b";
        byte[] key = "hello".getBytes();
        byte[] message = "world".getBytes();
        byte[] b = crypto.getHmacSha512(key, message);
        assertEquals(expected, Utility.getInstance().bytes2hex(b));
    }

}
