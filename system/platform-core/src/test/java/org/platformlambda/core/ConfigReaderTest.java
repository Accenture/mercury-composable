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
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigReaderTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigReaderTest.class);

    @BeforeAll
    public static void setup() {
        // guarantee base configuration is loaded
        AppConfigReader config = AppConfigReader.getInstance();
        log.info("Loaded {} key-values in base configuration", config.getCompositeKeyValues().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void envVarRenderingAppConfigReaderTest() {
        AppConfigReader config = AppConfigReader.getInstance();
        assertInstanceOf(Map.class, config.get("system"));
        String path = System.getenv("PATH");
        Map<String, Object> map = (Map<String, Object>) config.get("system");
        assertEquals(path, map.get("path"));
        assertTrue(config.isBaseConfig());
    }

    @SuppressWarnings("unchecked")
    @Test
    void envVarRenderingConfigReaderTest() throws IOException {
        // system will automatically find test.yaml if test.yml does not exist
        ConfigReader config = new ConfigReader("classpath:/test.yml");
        assertInstanceOf(Map.class, config.get("hello"));
        Map<String, Object> map = (Map<String, Object>) config.get("hello");
        // validate that reference to application.properties is resolved during config load
        Object name = map.get("name");
        assertEquals(Platform.getInstance().getName(), name);
        assertFalse(config.isBaseConfig());
    }

    @Test
    void environmentVarSubstitution() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        String path = System.getenv("PATH");
        assertEquals("path is "+path, reader.getProperty("hello.world"));
    }

    @Test
    void defaultValueForNonExistEnvVar() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
        assertEquals("text(http://127.0.0.1:8100) -> test", reader.getProperty("hello.no_env_var"));
    }

    @Test
    void systemPropertySubstitution() throws IOException {
        final String HELLO = "HELLO";
        System.setProperty("sample.system.property", HELLO);
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        assertEquals(HELLO, reader.getProperty("my.system.property"));
    }

    @Test
    void systemPropertyIsAlwaysAvailable() {
        final String NON_EXIST_PROPERTY = "parameter.not.found.in.application.properties";
        final String HELLO = "HELLO";
        System.setProperty(NON_EXIST_PROPERTY, HELLO);
        AppConfigReader config = AppConfigReader.getInstance();
        assertEquals(HELLO, config.getProperty(NON_EXIST_PROPERTY));
    }

    @Test
    void getValueFromParent() throws IOException {
        AppConfigReader parent = AppConfigReader.getInstance();
        String parentValue = parent.getProperty("cloud.connector");
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        String subValue = reader.getProperty("my.cloud.connector");
        assertEquals(parentValue, subValue);
    }

    @Test
    void getDefaultValue() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        String value = reader.getProperty("another.key");
        assertEquals("12345", value);
    }

    @Test
    void getNonExistEnvVariable() throws IOException {
        String TEXT = "hello";
        String ONE_HUNDRED = "100";
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        // property.one=${no.property} where no.property does not exist
        String value1 = reader.getProperty("property.one", TEXT);
        assertEquals(TEXT, value1);
        // without default value
        String value2 = reader.getProperty("property.one");
        assertNull(value2);
        // property.two=${no.property}100 will return "100"
        String value3 = reader.getProperty("property.two", TEXT);
        assertEquals(ONE_HUNDRED, value3);
        // without default value
        String value4 = reader.getProperty("property.two");
        assertEquals(ONE_HUNDRED, value4);
    }

    @Test
    void getDefaultValueWithControlCharacters() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        String value = reader.getProperty("property.three");
        assertEquals("someDefaultValue/{test1}/{test2}", value);
    }

    @SuppressWarnings("unchecked")
    @Test
    void dotFormatterTest() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
        Object o = reader.get("hello.world");
        assertEquals("some value", o);
        o = reader.get("hello.multiline");
        assertInstanceOf(String.class, o);
        assertTrue(o.toString().contains("\n"));
        List<String> lines = Utility.getInstance().split(o.toString(), "\n");
        assertEquals(2, lines.size());
        assertEquals("line one", lines.get(0));
        assertEquals("line two", lines.get(1));
        o = reader.get("hello.array");
        assertInstanceOf(ArrayList.class, o);
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
    void flattenMapTest() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.yml");
        Map<String, Object> map = Utility.getInstance().getFlatMap(reader.getMap());
        assertEquals("some value", map.get("hello.world"));
        assertEquals("hi", map.get("hello.array[0]"));
        assertEquals("this is great", map.get("hello.array[1]"));
        /*
         * Unlike properties file that converts values into strings,
         * YAML and JSON preserve original objects.
         */
        Object o = map.get("hello.number");
        assertInstanceOf(Integer.class, o);
        assertEquals(12345, o);
    }

    @SuppressWarnings("unchecked")
    @Test
    void appConfigTest() {
        // AppConfigReader will combine both application.properties and application.yml
        AppConfigReader reader = AppConfigReader.getInstance();
        // application.name is stored in "application.properties"
        assertEquals("platform-core", reader.getProperty("application.name"));
        // hello.world is stored in "application.yml"
        assertEquals("great", reader.get("hello.world"));
        // read complex map and list in application.yml
        Object v = reader.get("oh.hi");
        assertInstanceOf(Map.class, v);
        MultiLevelMap mm = new MultiLevelMap((Map<String, Object>) v);
        assertEquals(1, mm.getElement("great[0]"));
        assertNull(mm.getElement("great[1]"));
        assertEquals("great", mm.getElement("great[2].nice.way"));
        assertEquals("good", mm.getElement("great[2].nice.another[0]"));
    }

    @Test
    void parameterSubstitutionTest() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
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
    void dotFormatterSetTest() throws IOException {
        // generate random top level key
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String goodDay = uuid+".great.day";
        String goodArray = uuid+".array";
        String message = "test message";
        Integer input = 123456789;
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
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
        assertInstanceOf(Map.class, o);
        Map<String, Object> submap = (Map<String, Object>) o;
        assertEquals(2, submap.size());
        assertTrue(submap.containsKey("great"));
        assertTrue(submap.containsKey("array"));
    }

    @Test
    void resourceNotFound() {
        IOException ex = assertThrows(IOException.class, () -> new ConfigReader("classpath:/not-found.yaml"));
        assertEquals("classpath:/not-found.yaml not found", ex.getMessage());
    }

    @Test
    void fileNotFound() {
        IOException ex = assertThrows(IOException.class, () -> new ConfigReader("file:/not-found.yaml"));
        assertEquals("file:/not-found.yaml not found", ex.getMessage());
    }

    @Test
    void jsonReadTest() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.json");
        assertEquals(2, reader.getMap().size());
        assertEquals("world", reader.get("hello"));
        assertEquals("message", reader.get("test"));
    }

    @Test
    void getPropertyFromEnv() {
        AppConfigReader config = AppConfigReader.getInstance();
        String value = config.getProperty("system.path");
        String path = System.getenv("PATH");
        assertEquals(path, value);
    }

    @Test
    void getPropertyFromEnvFromAnotherConfig() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
        Object value = reader.get("hello.path");
        String path = System.getenv("PATH");
        assertEquals(path, value);
    }

    @Test
    void singleLevelLoopErrorTest() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        String value = reader.getProperty("recursive.key");
        // In case of config loop, the system will not resolve a parameter value.
        assertNull(value);
    }

    @Test
    void multiLevelLoopErrorTest() {
        AppConfigReader config = AppConfigReader.getInstance();
        Object value1 = config.get("looping.test.1");
        assertEquals("1000", value1);
        // test recursion
        Object value2 = config.get("looping.test.3");
        assertEquals("hello hello ", value2);
    }

    @Test
    void defaultValueTest() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
        String value = reader.getProperty("test.no.value", "hello world");
        assertEquals("hello world", value);
    }
    @Test
    void defaultValueInRefTest() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
        String value = reader.getProperty("test.default");
        assertEquals("hello 1000", value);
    }

    @Test
    void noDefaultValueInRefTest() throws IOException {
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
        String value = reader.getProperty("test.no_default");
        // when there is no default value in the reference, it will return empty string as the default value.
        assertEquals("hello world", value);
    }

    @Test
    void compositeKeyValueTest() throws IOException {
        AppConfigReader config = AppConfigReader.getInstance();
        // prove that the configuration key-values are fully resolved from system properties and environment variables
        Map<String, Object> raw = config.getMap();
        MultiLevelMap multi = new MultiLevelMap(raw);
        assertNull(multi.getElement("recursive.key"));
        // prove that config.get() method resolves value substitution
        assertNull(config.get("recursive.key"));
        // prove that compositeKeyValues return the same value as config.get() method
        Map<String, Object> map = config.getCompositeKeyValues();
        assertEquals(config.get("application.feature.route.substitution"),
                            map.get("application.feature.route.substitution"));
        // and it works for secondary configuration file too
        ConfigReader reader = new ConfigReader("classpath:/test.yaml");
        Map<String, Object> kv = reader.getCompositeKeyValues();
        String port = config.getProperty("server.port");
        assertEquals(port+" is server port", kv.get("hello.location[3]"));
        assertEquals("Server port is "+port, kv.get("hello.location[4]"));
    }

    @Test
    void activeProfileTest() {
        AppConfigReader config = AppConfigReader.getInstance();
        String testPara = config.getProperty("test.parameter");
        Object testYaml = config.get("test.yaml");
        assertEquals("hello world", testPara);
        assertEquals(100, testYaml);
    }

    @Test
    void multiEnvVarTest() throws IOException {
        AppConfigReader config = AppConfigReader.getInstance();
        String cloudConnector = config.getProperty("cloud.connector");
        String serverPort = config.getProperty("server.port");
        String componentScan = config.getProperty("web.component.scan");
        final String expected = "1 " + cloudConnector + ", 2 " +
                                serverPort + ", 3 " + componentScan + ", 4 12345, 5 .";
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        Object value = reader.getProperty("multiple.env.vars");
        assertEquals(expected, value);
    }

    @Test
    void multiEnvVarWithErrorTest() throws IOException {
        AppConfigReader config = AppConfigReader.getInstance();
        String cloudConnector = config.getProperty("cloud.connector");
        String componentScan = config.getProperty("web.component.scan");
        // server.port is not resolved due to config error and the last environment variable signature is incomplete
        final String expected = "1 " + cloudConnector + ", 2 ${server.port, 3 " + componentScan + ", 4 12345, 5${none";
        ConfigReader reader = new ConfigReader("classpath:/test.properties");
        Object value = reader.getProperty("error.multiple.env.vars");
        assertEquals(expected, value);
    }

}
