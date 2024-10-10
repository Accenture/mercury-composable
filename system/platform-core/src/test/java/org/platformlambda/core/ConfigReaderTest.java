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

package org.platformlambda.core;

import org.junit.BeforeClass;
import org.junit.Test;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class ConfigReaderTest {

    @BeforeClass
    public static void setup() {
        ConfigReader.setBaseConfig(AppConfigReader.getInstance());
    }

    @Test
    public void environmentVarSubstitution() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.properties");
        String path = System.getenv("PATH");
        assertEquals(path, reader.getProperty("hello.world"));
    }

    @Test
    public void defaultValueForNonExistEnvVar() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        assertEquals("text(http://127.0.0.1:8100) -> test", reader.getProperty("hello.no_env_var"));
    }

    @Test
    public void systemPropertySubstitution() throws IOException {
        final String HELLO = "HELLO";
        System.setProperty("sample.system.property", HELLO);
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.properties");
        assertEquals(HELLO, reader.getProperty("my.system.property"));
    }

    @Test
    public void systemPropertyIsAlwaysAvailable() {
        final String NON_EXIST_PROPERTY = "parameter.not.found.in.application.properties";
        final String HELLO = "HELLO";
        System.setProperty(NON_EXIST_PROPERTY, HELLO);
        AppConfigReader config = AppConfigReader.getInstance();
        assertEquals(HELLO, config.getProperty(NON_EXIST_PROPERTY));
    }

    @Test
    public void getValueFromParent() throws IOException {
        AppConfigReader parent = AppConfigReader.getInstance();
        String parentValue = parent.getProperty("cloud.connector");
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.properties");
        String subValue = reader.getProperty("my.cloud.connector");
        assertEquals(parentValue, subValue);
    }

    @Test
    public void getDefaultValue() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.properties");
        String value = reader.getProperty("another.key");
        assertEquals("12345", value);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void dotFormatterTest() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        Object o = reader.get("hello.world");
        assertEquals("some value", o);
        o = reader.get("hello.multiline");
        assertTrue(o instanceof String);
        assertTrue(o.toString().contains("\n"));
        List<String> lines = Utility.getInstance().split(o.toString(), "\n");
        assertEquals(2, lines.size());
        assertEquals("line one", lines.get(0));
        assertEquals("line two", lines.get(1));
        o = reader.get("hello.array");
        assertTrue(o instanceof ArrayList);
        List<String> elements = (List<String>) o;
        assertEquals(2, elements.size());
        assertEquals("hi", elements.get(0));
        assertEquals("this is great", elements.get(1));
        o = reader.get("hello.array[0]");
        assertEquals("hi", o);
        o = reader.get("hello.array[1]");
        assertEquals("this is great", o);
    }

    @Test
    public void flattenMapTest() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yml");
        Map<String, Object> map = Utility.getInstance().getFlatMap(reader.getMap());
        assertEquals("some value", map.get("hello.world"));
        assertEquals("hi", map.get("hello.array[0]"));
        assertEquals("this is great", map.get("hello.array[1]"));
        /*
         * Unlike properties file that converts values into strings,
         * YAML and JSON preserve original objects.
         */
        Object o = map.get("hello.number");
        assertTrue(o instanceof Integer);
        assertEquals(12345, o);
    }

    @Test
    public void appConfigTest() {
        // AppConfigReader will combine both application.properties and application.yml
        AppConfigReader reader = AppConfigReader.getInstance();
        // application.name is stored in "application.properties"
        assertEquals("platform-core", reader.getProperty("application.name"));
        // hello.world is stored in "application.yml"
        assertEquals("great", reader.get("hello.world"));
    }

    @Test
    public void parameterSubstitutionTest() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        assertEquals("platform-core", reader.getProperty("hello.name"));
        AppConfigReader config = AppConfigReader.getInstance();
        assertEquals("8100", reader.getProperty("hello.location[0]"));
        assertEquals("http://127.0.0.1:"+config.getProperty("server.port")+"/info",
                                reader.getProperty("hello.location[1]"));
        assertEquals(100, reader.get("hello.location[2].world.blvd"));
        assertEquals(config.getProperty("server.port")+" is server port",
                                reader.getProperty("hello.location[3]"));
        assertEquals("Server port is "+config.getProperty("server.port"),
                                reader.getProperty("hello.location[4]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void dotFormatterSetTest() throws IOException {
        // generate random top level key
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String goodDay = uuid+".great.day";
        String goodArray = uuid+".array";
        String message = "test message";
        Integer input = 123456789;
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        int size = reader.getMap().size();
        MultiLevelMap formatter = new MultiLevelMap(reader.getMap());
        formatter.setElement(goodDay, input).setElement(goodArray+"[1]", message);
        Object o = formatter.getElement(goodDay);
        assertEquals(input, o);
        // confirm added only one key at the top level
        assertEquals(size+1, formatter.getMap().size());
        assertNull(formatter.getElement(goodArray+"[0]"));
        assertEquals(message, formatter.getElement(goodArray+"[1]"));
        o = formatter.getElement(uuid);
        assertTrue(o instanceof Map);
        Map<String, Object> submap = (Map<String, Object>) o;
        assertEquals(2, submap.size());
        assertTrue(submap.containsKey("great"));
        assertTrue(submap.containsKey("array"));
    }

    @Test
    public void resourceNotFound() {
        IOException ex = assertThrows(IOException.class, () -> {
            ConfigReader reader = new ConfigReader();
            reader.load("classpath:/notfound.yaml");
        });
        assertEquals("classpath:/notfound.yaml not found", ex.getMessage());
    }

    @Test
    public void fileNotFound() {
        IOException ex = assertThrows(IOException.class, () -> {
            ConfigReader reader = new ConfigReader();
            reader.load("file:/notfound.yaml");
        });
        assertEquals("file:/notfound.yaml not found", ex.getMessage());
    }

    @Test
    public void jsonReadTest() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.json");
        assertEquals(2, reader.getMap().size());
        assertEquals("world", reader.get("hello"));
        assertEquals("message", reader.get("test"));
    }

    @Test
    public void getPropertyFromEnv() {
        AppConfigReader config = AppConfigReader.getInstance();
        String value = config.getProperty("system.path");
        String path = System.getenv("PATH");
        assertEquals(path, value);
    }

    @Test
    public void getPropertyFromEnvFromAnotherConfig() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        Object value = reader.get("hello.path");
        String path = System.getenv("PATH");
        assertEquals(path, value);
    }

    @Test
    public void singleLevelLoopErrorTest() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.properties");
        String value = reader.getProperty("recursive.key");
        // In case of config loop, the system will not resolve a parameter value.
        assertNull(value);
    }

    @Test
    public void multiLevelLoopErrorTest() {
        AppConfigReader config = AppConfigReader.getInstance();
        Object value = config.get("looping.test.1");
        assertEquals("1000", value);
    }

    @Test
    public void defaultValueTest() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        String value = reader.getProperty("test.no.value", "hello world");
        assertEquals("hello world", value);
    }
    @Test
    public void defaultValueInRefTest() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        String value = reader.getProperty("test.default");
        assertEquals("hello 1000", value);
    }

    @Test
    public void noDefaultValueInRefTest() throws IOException {
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        String value = reader.getProperty("test.no_default");
        // when there is no default value in the reference, it will return empty string as the default value.
        assertEquals("hello world", value);
    }

    @Test
    public void compositeKeyValueTest() throws IOException {
        AppConfigReader config = AppConfigReader.getInstance();
        // prove that config.getMap() method returns raw data without value substitution
        Map<String, Object> raw = config.getMap();
        MultiLevelMap multi = new MultiLevelMap(raw);
        assertEquals("${recursive.key}", multi.getElement("recursive.key"));
        // prove that config.get() method resolves value substitution
        assertNull(config.get("recursive.key"));
        // prove that compositeKeyValues return the same value as config.get() method
        Map<String, Object> map = config.getCompositeKeyValues();
        assertEquals(config.get("application.feature.route.substitution"),
                            map.get("application.feature.route.substitution"));
        // and it works for secondary configuration file too
        ConfigReader reader = new ConfigReader();
        reader.load("classpath:/test.yaml");
        Map<String, Object> kv = reader.getCompositeKeyValues();
        String port = config.getProperty("server.port");
        assertEquals(port+" is server port", kv.get("hello.location[3]"));
        assertEquals("Server port is "+port, kv.get("hello.location[4]"));
    }

    @Test
    public void activeProfileTest() {
        AppConfigReader config = AppConfigReader.getInstance();
        String testPara = config.getProperty("test.parameter");
        Object testYaml = config.get("test.yaml");
        assertEquals("hello world", testPara);
        assertEquals(100, testYaml);
    }

}
