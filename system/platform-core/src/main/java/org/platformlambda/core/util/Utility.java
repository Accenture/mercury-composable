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

package org.platformlambda.core.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.websocket.server.WsEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Utility {
    private static final Logger log = LoggerFactory.getLogger(Utility.class);
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String ISO_MS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern(ISO_DATE_FORMAT);
    private static final DateTimeFormatter ISO_DATE_MS = DateTimeFormatter.ofPattern(ISO_MS_FORMAT);
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter SQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HTML_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");
    private static final DateTimeFormatter HTML_GMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");
    private static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmssSSS";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);
    private static final String SQL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final DateTimeFormatter SQL_TIMESTAMP = DateTimeFormatter.ofPattern(SQL_TIMESTAMP_FORMAT);
    private static final ZoneId UTC_TIME = ZoneId.of("UTC");
    private static final String NESTED_EXCEPTION_MARKER = "** BEGIN NESTED EXCEPTION **";
    private static final String NESTED_EXCEPTION_START = "MESSAGE:";
    private static final String NESTED_EXCEPTION_END = "STACKTRACE:";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String BOOT_INF_LIB = "BOOT-INF/lib";
    private static final String META_INF_MAVEN = "META-INF/maven";
    private static final String META_INF = "META-INF";
    private static final String JAR = "jar";
    private static final String PROPERTIES = "properties";
    private static final String APP_VERSION = "info.app.version";
    private static final String DEFAULT_APP_VERSION = "1.0.0";
    private static final String[] RESERVED_FILENAMES = {"thumbs.db"};
    private static final String[] RESERVED_EXTENSIONS = {
            ".con", ".prn", ".aux", ".nul", ".com", ".exe",
            ".com1", ".com2", ".com3", ".com4", ".com5", ".com6", ".com7", ".com8", ".com9",
            ".lpt1", ".lpt2", ".lpt3", ".lpt4", ".lpt5", ".lpt6", ".lpt7", ".lpt8", ".lpt9"};
    private static final String INVALID_HEX = "Invalid hex string";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final String ZEROS = "0000000000000000";
    private static final String[] intranetPrefixes = {
            "10.", "192.168.",
            "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31."
    };
    private static final long ONE_SECOND_MS = 1000;
    private static final long ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
    private static final long ONE_HOUR_MS = 60 * ONE_MINUTE_MS;
    private static final long ONE_DAY_MS = 24 * ONE_HOUR_MS;
    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 60 * ONE_MINUTE;
    private static final int ONE_DAY = 24 * ONE_HOUR;
    private static final Object SINGLETON = new Object[0];
    private static String appVersion = "unknown";
    private final List<String> libs = new ArrayList<>();
    private boolean loaded = false;
    private static final Utility instance = new Utility();

    private Utility() {
        // singleton
    }

    public static Utility getInstance() {
        return instance;
    }

    private List<String> getJarsFromSpringBootPackage() {
        List<String> list = new ArrayList<>();
        try (ScanResult scan = new ClassGraph().acceptPathsNonRecursive(BOOT_INF_LIB).scan()) {
            ResourceList resources = scan.getResourcesWithExtension(JAR);
            for (Resource r: resources) {
                String name = r.getPath();
                name = name.substring(0, name.length() - JAR.length() - 1);
                list.add(name.contains("/")? name.substring(name.lastIndexOf('/') + 1) : name);
            }
        }
        return list;
    }

    private List<String> getMavenProperties() {
        List<String> list = new ArrayList<>();
        try (ScanResult scan = new ClassGraph().acceptPaths(META_INF_MAVEN).scan()) {
            ResourceList resources = scan.getResourcesWithExtension(PROPERTIES);
            for (Resource r: resources) {
                String name = r.getPath();
                if (name.endsWith("/pom.properties")) {
                    Properties p = new Properties();
                    p.load(new ByteArrayInputStream(stream2bytes(r.open())));
                    String groupId = p.getProperty("groupId");
                    String artifactId = p.getProperty("artifactId");
                    String version = p.getProperty("version");
                    if (groupId != null && artifactId != null && version != null) {
                        list.add(groupId+" / "+artifactId+"-"+version);
                    }
                }
            }
        } catch (IOException e) {
            // ok to ignore
        }
        return list;
    }

    private void scanLibInfo() {
        if (!loaded) {
            synchronized (SINGLETON) {
                AppConfigReader reader = AppConfigReader.getInstance();
                appVersion = reader.getProperty(APP_VERSION, DEFAULT_APP_VERSION);
                List<String> list = new ArrayList<>();
                List<String> jars = getJarsFromSpringBootPackage();
                if (jars.isEmpty()) {
                    List<String> pom = getMavenProperties();
                    if (!pom.isEmpty()) {
                        libs.add("--- "+META_INF_MAVEN+" ---");
                        list.addAll(pom);
                    }
                } else {
                    libs.add("--- "+BOOT_INF_LIB+" ---");
                    list.addAll(jars);
                }
                if (list.size() > 1) {
                    Collections.sort(list);
                }
                if (!list.isEmpty()) {
                    try (ScanResult scan = new ClassGraph().acceptPathsNonRecursive(META_INF).scan()) {
                        ResourceList resources = scan.getResourcesWithExtension(".MF");
                        for (Resource r: resources) {
                            if (r.getPath().equals("META-INF/MANIFEST.MF")) {
                                Properties p = new Properties();
                                p.load(new ByteArrayInputStream(stream2bytes(r.open())));
                                String v = p.getProperty("Implementation-Version");
                                if (v != null) {
                                    appVersion = v;
                                }
                                break;
                            }
                        }
                    } catch (IOException e) {
                        // ok to ignore
                    }
                }
                libs.addAll(list);
                if (libs.isEmpty()) {
                    libs.add("Are you running the app from classpath?");
                }
            }
            loaded = true;
        }
    }

    public String getVersion() {
        scanLibInfo();
        return appVersion;
    }

    public List<String> getLibraryList() {
        scanLibInfo();
        return libs;
    }

    /**
     * Zero fill a long number by adding leading zeros
     *
     * @param seq is the number to zero fill
     * @param max number indicates the largest number for counting digits
     * @return zero filled number in text
     */
    public String zeroFill(int seq, int max) {
        int len = String.valueOf(max).length();
        String value = String.valueOf(seq);
        return value.length() < len? ZEROS.substring(0, len - value.length()) + value : value;
    }

    /**
     * Zero fill an integer by adding leading zeros
     *
     * @param seq is the number to zero fill
     * @param max number indicates the largest number for counting digits
     * @return zero filled number in text
     */
    public String zeroFill(long seq, long max) {
        int len = String.valueOf(max).length();
        String value = String.valueOf(seq);
        return value.length() < len? ZEROS.substring(0, len - value.length()) + value : value;
    }

    /**
     * Get a UUID
     *
     * @return uuid4 random value
     */
    public String getUuid4() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get a UUID without the hyphen separator character
     *
     * @return uuid random value
     */
    public String getUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Get a UUID with current day as the prefix (YYYYMMDD format)
     *
     * @return uuid random value
     */
    public String getDateUuid() {
        return getDateOnly(new Date())+getUuid();
    }

    /////////////////////////////////////////////////////
    // Simple string splitter without overheads of regex
    /////////////////////////////////////////////////////

    /**
     * Split a string using a set of separators
     * Skips empty values.
     *
     * @param str the input string
     * @param chars the characters to be used as separators
     * @return list
     */
    public List<String> split(String str, String chars) {
        return split(str, chars, false);
    }

    /**
     * Split a string using a set of separators
     *
     * @param str the input string
     * @param chars the characters to be used as separators
     * @param empty if true, return empty elements
     * @return list
     */
    public List<String> split(String str, String chars, boolean empty) {
        List<String> rv = new ArrayList<>();
        if (str == null || chars == null) return rv;
        StringBuilder sb = new StringBuilder();
        boolean found;
        for (int i=0; i < str.length(); i++) {
            found = false;
            for (int j=0; j < chars.length(); j++) {
                if (str.charAt(i) == chars.charAt(j)) {
                    if (!sb.isEmpty()) {
                        rv.add(sb.toString());
                    } else if (empty) {
                        rv.add("");
                    }
                    // reset buffer
                    sb.setLength(0);
                    found = true;
                    break;
                }
            }
            if (!found) sb.append(str.charAt(i));
        }
        if (!sb.isEmpty()) rv.add(sb.toString());
        return rv;
    }

    ////////////////////////////////////////
    // Convenient properties file utilities
    ////////////////////////////////////////

    /**
     * Hierarchical data of a map to be converted into properties like "this.is.a.key"
     *
     * @param src map
     * @return flat map
     */
    public Map<String, Object> getFlatMap(Map<String, Object> src) {
        Map<String, Object> target = new HashMap<>();
        getFlatMap(null, src, target);
        return target;
    }

    @SuppressWarnings("unchecked")
    private void getFlatMap(String prefix, Map<String, Object> src, Map<String, Object> target) {
        for (var entry: src.entrySet()) {
            String key = prefix == null? entry.getKey() : prefix + "." + entry.getKey();
            Object v = entry.getValue();
            if (v instanceof Map) {
                getFlatMap(key, (Map<String, Object>) v, target);
            } else if (v instanceof List) {
                int n = 0;
                for (Object o: (List<Object>) v) {
                    String next = key +"["+n+"]";
                    n++;
                    if (o instanceof Map) {
                        getFlatMap(next, (Map<String, Object>) o, target);
                    } else if (o instanceof List) {
                        getFlatList(next, (List<Object>) o, target);
                    } else if (o != null) {
                        target.put(next, o);
                    }
                }
            } else if (v != null) {
                target.put(key, v);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void getFlatList(String prefix, List<Object> src, Map<String, Object> target) {
        int n = 0;
        for (Object v: src) {
            String key = prefix+"["+n+"]";
            n++;
            if (v instanceof Map) {
                getFlatMap(key, (Map<String, Object>) v, target);
            } else if (v instanceof List) {
                getFlatList(key, (List<Object>) v, target);
            } else {
                target.put(key, v);
            }
        }
    }

    //////////////////////////////////
    // Convenient timestamp utilities
    //////////////////////////////////

    public String getTimestamp() {
        return getTimestamp(System.currentTimeMillis());
    }

    public String getTimestamp(long time) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), UTC_TIME);
        return zdt.format(TIMESTAMP);
    }

    public String getSqlDate(java.sql.Date date) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), UTC_TIME);
        return zdt.format(SQL_DATE);
    }

    public String getSqlTimestamp(java.sql.Timestamp time) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time.getTime()), UTC_TIME);
        return zdt.format(SQL_TIMESTAMP);
    }

    public String getDateOnly(Date date) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), UTC_TIME);
        return zdt.format(DATE_ONLY);
    }

    public String getAmazonDate(Date date) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), UTC_TIME);
        return zdt.format(DATE_TIME).replace('-', 'T')+"Z";
    }

    /**
     * Generate a RFC 7231 timestamp
     * <p>
     * @param date object
     * @return timestamp for use in an HTTP header (Example: Tue, 11 May 2021 03:42:46 GMT)
     */
    public String getHtmlDate(Date date) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), UTC_TIME);
        return zdt.format(HTML_DATE);
    }

    /**
     * Parse a RFC 7231 HTML timestamp
     * <p>
     * @param timestamp from an HTTP header
     * @return converted date object
     */
    public Date getHtmlDate(String timestamp) {
        // change GMT to "Z" timezone for compatibility with Java 1.8
        ZonedDateTime dt = ZonedDateTime.parse(timestamp.replace("GMT", "-0000"), HTML_GMT);
        return Date.from(dt.toInstant());
    }

    public long timestamp2ms(String timestamp) {
        // timestamp is formatted as yyyyMMddHHmmssSSS
        if (timestamp == null || timestamp.length() != TIMESTAMP_FORMAT.length()) {
            return 0;
        }
        int year = str2int(timestamp.substring(0, 4));
        int month = str2int(timestamp.substring(4, 6));
        int day = str2int(timestamp.substring(6, 8));
        int hour = str2int(timestamp.substring(8, 10));
        int minute = str2int(timestamp.substring(10, 12));
        int second = str2int(timestamp.substring(12, 14));
        int ms = str2int(timestamp.substring(14));
        ZonedDateTime zdt = ZonedDateTime.of(year, month, day, hour, minute, second,
                                ms * 1000000, UTC_TIME);
        return zdt.toInstant().toEpochMilli();
    }

    /////////////////////////////
    // Convenient file utilities
    /////////////////////////////

    public byte[] stream2bytes(InputStream stream) {
        return stream2bytes(stream, true);
    }

    public byte[] stream2bytes(InputStream stream, boolean closeStream) {
        return stream2bytes(stream, closeStream, -1);
    }

    public byte[] stream2bytes(InputStream stream, boolean closeStream, int maxSize) {
        if (stream == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;
        int total = 0;
        byte[] buffer = new byte[1024];
        if (stream instanceof ByteArrayInputStream in) {
            while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, len) ;
            }
        } else {
            BufferedInputStream bin = (stream instanceof BufferedInputStream buffered) ?
                                        buffered : new BufferedInputStream(stream);
            try {
                while ((len = bin.read(buffer, 0, buffer.length)) != -1) {
                    total += len;
                    if (maxSize > 0 && total > maxSize) {
                        throw new IllegalArgumentException("input larger than max of "+maxSize+" bytes");
                    }
                    out.write(buffer, 0, len) ;
                }
                if (closeStream) bin.close();
                bin = null;
            } catch (IOException e) {
                // ignore
            } finally {
                if (bin != null && closeStream)
                    try {
                        bin.close();
                    } catch (IOException e) {
                        // does not matter
                    }
            }
        }
        return out.toByteArray();
    }

    public String stream2str(InputStream stream) {
        return getUTF(stream2bytes(stream));
    }
    
    public boolean str2file(File f, String s) {
        return str2file(f, s, false);
    }

    public boolean str2file(File f, String s, boolean append) {
        return bytes2file(f, getUTF(s), append);
    }

    public boolean bytes2file(File f, byte[] b) {
        return bytes2file(f, b, false);
    }

    public boolean bytes2file(File f, byte[] b, boolean append) {
        byte[] content = file2bytes(f);
        if (!append && Arrays.equals(b, content)) {
            return true;
        } else {
            try (FileOutputStream out = new FileOutputStream(f, append)) {
                out.write(b);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public String file2str(File f) {
        return getUTF(file2bytes(f));
    }

    public byte[] file2bytes(File f) {
        if (f != null) {
            try {
                FileInputStream in = new FileInputStream(f);
                return stream2bytes(in);
            } catch (FileNotFoundException e) {
                // does not matter
            }
        }
        return new byte[0];
    }

    public void cleanupDir(File dir) {
        cleanupDir(dir, false);
    }

    public void cleanupDir(File dir, boolean keep) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f: files) {
                    if (f.isDirectory()) {
                        cleanupDir(f, false);
                    } else {
                        if (f.delete()) {
                            log.debug("File {} deleted", f);
                        }
                    }
                }
            }
            if (!keep) {
                if (dir.delete()) {
                    log.debug("Folder {} deleted", dir);
                }
            }
        }
    }

    ////////////////////////////////////////////////
    // Useful integer and long conversion utilities
    ////////////////////////////////////////////////

    public int bytes2int(byte[] b) {
        if (b == null || b.length != 4) return -1;
        int val = 0;
        for (int i=0; i < 4; ++i) {
            val *= 256;
            if ((b[i] & 0x80) == 0x80) {
                val += 128;
            }
            val += (b[i] & 0x7F) ;
        }
        return val;
    }

    public byte[] int2bytes(int val) {
        byte[] results = new byte[4];

        for (int idx=3; idx >=0; --idx) {
            results[idx] = (byte) (val & 0xFF);
            val = val >> 8;
        }
        return results;
    }

    public int str2int(String str) {
        if (str == null || str.isEmpty()) {
            return -1;
        }
        try {
            // drop decimal point
            int dot = str.indexOf('.');
            return Integer.parseInt(dot > 0 ? str.substring(0, dot) : str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public byte[] long2bytes(long val) {
        byte[] results = new byte[8];
        for (int idx=7; idx >=0; --idx) {
            results[idx] = (byte) (val & 0xFF);
            val = val >> 8;
        }
        return results;
    }

    public long bytes2long(byte[] b) {
        if (b == null || b.length != 8) return -1;
        long val = 0;
        for (int i=0 ; i < 8; ++i) {
            val *= 256L ;
            if ((b[i] & 0x80) == 0x80) {
                val += 128L ;
            }
            val += (b[i] & 0x7F) ;
        }
        return val;
    }

    public long str2long(String str) {
        if (str == null || str.isEmpty()) {
            return -1;
        }
        try {
            // drop decimal point
            int dot = str.indexOf('.');
            return Long.parseLong(dot > 0 ? str.substring(0, dot) : str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public float str2float(String str) {
        if (str == null || str.isEmpty()) {
            return -1.0f;
        }
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return -1.0f;
        }
    }

    public double str2double(String str) {
        if (str == null || str.isEmpty()) {
            return -1.0d;
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return -1.0d;
        }
    }

    public boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        if (str.length() > 1 && str.startsWith("-")) {
            return isDigits(str.substring(1));
        } else {
            return isDigits(str);
        }
    }

    public boolean isPoJo(Object o) {
        if (o == null || o instanceof Map || o instanceof byte[] ||
                o instanceof String || o instanceof Number || o instanceof Boolean) {
            return false;
        }
        String clsName = o.getClass().getName();
        return clsName.contains(".") && !clsName.startsWith("java.");
    }

    ///////////////////////////////////////
    // Conversion between string and bytes
    ///////////////////////////////////////

    public byte[] getUTF(String str) {
        if (str == null || str.isEmpty()) return new byte[0];
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public String getUTF(byte[] b) {
        if (b == null || b.length == 0) return "";
        return new String(b, StandardCharsets.UTF_8);
    }

    public Map<String, Object> stackTraceToMap(String stackTrace) {
        Map<String, Object> result = new HashMap<>();
        List<String> stack = new ArrayList<>();
        List<String> lines = split(stackTrace, "\r\n");
        lines.forEach(line -> stack.add(line.trim()));
        result.put("stack", stack);
        return result;
    }

    public String getStackTrace(Throwable ex) {
        try (StringWriter out = new StringWriter(); PrintWriter writer = new PrintWriter(out)) {
            ex.printStackTrace(writer);
            return out.toString();
        } catch (IOException e) {
            // best effort is to keep the exception message
            return ex.getMessage();
        }
    }

    public String getSimpleRoute(String route) {
        int at = route.indexOf('@');
        return at == -1? route : route.substring(0, at);
    }

    /**
     * Valid service name (routing path and project ID)
     * must contain only 0-9, a-z, period, underscore and hyphen.
     *
     * @param str for routing path or project ID
     * @return true if valid
     */
    public boolean validServiceName(String str) {
        if (str == null || str.isEmpty()) return false;
        if (str.startsWith(".") || str.startsWith("_") || str.startsWith("-")
                || str.contains("..")
                || str.endsWith(".") || str.endsWith("_") || str.endsWith("-")) return false;
        for (int i=0; i < str.length(); i++) {
            if (str.charAt(i) >= '0' && str.charAt(i) <= '9') continue;
            if (str.charAt(i) >= 'a' && str.charAt(i) <= 'z') continue;
            if (str.charAt(i) == '.' || str.charAt(i) == '_' || str.charAt(i) == '-') continue;
            return false;
        }
        return true;
    }

    public String filteredServiceName(String name) {
        String str = name.toLowerCase();
        boolean dot = true;
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < str.length(); i++) {
            if (dot) {
                if (str.charAt(i) == '.') {
                    continue;
                } else {
                    dot = false;
                }
            }
            if (str.charAt(i) >= '0' && str.charAt(i) <= '9') {
                sb.append(str.charAt(i));
                continue;
            }
            if (str.charAt(i) >= 'a' && str.charAt(i) <= 'z') {
                sb.append(str.charAt(i));
                continue;
            }
            if (str.charAt(i) == '.' || str.charAt(i) == '_' || str.charAt(i) == '-') {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    public boolean reservedFilename(String name) {
        for (String filename: RESERVED_FILENAMES) {
            if (name.equals(filename)) {
                return true;
            }
        }
        return false;
    }

    public boolean reservedExtension(String name) {
        for (String ext: RESERVED_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public String date2str(Date date) {
        return date2str(date, false);
    }

    public String date2str(Date date, boolean upToSeconds) {
        // just in case input is SQL date or time
        if (date instanceof java.sql.Date || date instanceof java.sql.Time) {
            return date.toString();
        }
        long time = upToSeconds? date.getTime() / 1000 * 1000 : date.getTime();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), UTC_TIME);
        return zdt.format(DateTimeFormatter.ISO_INSTANT);
    }

    public Date str2date(String str) {
        return str2date(str, false);
    }

    public String getLocalTimestamp() {
        return getLocalTimestamp(new Date().getTime());
    }

    public String getLocalTimestamp(long ms) {
        LocalDateTime local = new Date(ms).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return local.toString().replace('T', ' ');
    }

    public LocalDate str2localDate(String str) {
        if (str == null) {
            return new Date(0).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (str.endsWith("Z") || str.contains("+")) {
            Date date = str2date(str);
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            if (str.length() >= 16 && str.charAt(10) != 'T') {
                str = str.substring(0, 10) + "T" + str.substring(11);
            }
            try {
                return LocalDate.parse(str);
            } catch (DateTimeParseException e) {
                return new Date(0).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
    }

    public LocalDateTime str2LocalDateTime(String str) {
        return str2LocalDateTime(str, false);
    }

    public LocalDateTime str2LocalDateTime(String str, boolean throwException) {
        if (str == null) {
            if (throwException) {
                throw new IllegalArgumentException("time string cannot be null");
            } else {
                return new Date(0).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        if (str.endsWith("Z") || str.contains("+")) {
            Date date = str2date(str);
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else {
            /*
             * Support multiple variance of ISO-8601 without time zone
             *
             * 1. 2015-01-06
             * 2. 2015-01-06 01:02
             * 3. 2015-01-06 01:02:03
             * 4. 2015-01-06 01:02:03.123
             * 5. 2015-01-06T01:02
             * 6. 2015-01-06T01:02:03
             * 7. 2015-01-06T01:02:03.123
             */
            if (str.length() >= 16 && str.charAt(10) != 'T') {
                str = str.substring(0, 10) + "T" + str.substring(11);
            }
            try {
                return LocalDateTime.parse(str);
            } catch (DateTimeParseException e) {
                if (throwException) {
                    throw e;
                } else {
                    return new Date(0).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        }
    }

    public Date str2date(String str, boolean throwException) {
        if (isDigits(str)) {
            return new Date(Long.parseLong(str));
        }
        /*
         * Support multiple variance of ISO-8601
         * (Note that when time zone is not given, it is set to +0000)
         *
         * 1. 2015-01-06
         * 2. 2015-01-06 01:02:03
         * 3. 2015-01-06T01:02:03
         * 4. 2015-01-06T01:02:03Z
         * 5. 2015-01-06T01:02:03+0000
         * 6. 2015-01-06T01:02:03+00:00
         * 7. 2015-01-06T01:02:03.123Z
         * 8. 2015-01-06T01:02:03.123+0000
         * 9. 2015-01-06T01:02:03.123+00:00
         *
         * DO NOT CHANGE the string substitution sequence below
         */
        // zero fill time and skip fields
        if (str.length() == 10) {
            str += "T00:00:00+0000";
        }
        // change SPACE to T if needed
        if (str.length() > 11 && str.charAt(10) == ' ') {
            str = str.substring(0, 10) + "T" + str.substring(11);
        }
        // use UTC timezone if not specified
        if (str.length() == 19) {
            str += "+0000";
        }
        // normalize UTC "Z" indicator to +0000
        if (str.endsWith("Z")) {
            str = str.substring(0, str.length()-1)+"+0000";
        }
        // precision up to milliseconds only and drop microseconds if any
        int dot = str.indexOf('.');
        if (dot == 19) {
            int sep = str.indexOf('+', 19);
            if (sep == -1) {
                sep = str.indexOf('-', 19);
            }
            String ms = normalizeMs(sep > 0? str.substring(dot, sep) : str.substring(dot));
            // remove colon from timezone
            String timezone = sep == -1? "+0000" : str.substring(sep).replace(":", "");
            str = str.substring(0, dot) + ms + timezone;
        }
        // parse the normalized time string
        try {
            ZonedDateTime zdt = dot==19? ZonedDateTime.parse(str, ISO_DATE_MS) : ZonedDateTime.parse(str, ISO_DATE);
            return Date.from(zdt.toInstant());
        } catch (IllegalArgumentException | DateTimeParseException e) {
            if (throwException) {
                throw new IllegalArgumentException(e.getMessage());
            } else {
                return new Date(0);
            }
        }
    }

    public boolean isDigits(String str) {
        if (str == null) {
            return false;
        }
        for (int i=0; i < str.length(); i++) {
            if (str.charAt(i) >= '0' && str.charAt(i) <= '9') continue;
            return false;
        }
        return !str.isEmpty();
    }

    private String normalizeMs(String s) {
        String result = s.length() < 4? s + "000" : s;
        return result.length() == 4? result : result.substring(0, 4);
    }

    //////////////////////////////////////
    // Resolve root cause of an exception
    //////////////////////////////////////

    public Throwable getRootCause(Throwable exception) {
        if (exception.getCause() != null) {
            return getRootCause(exception.getCause());
        }
        // just in case the exception is expressed as a string
        String message = exception.getMessage();
        if (message != null) {
            int nested = message.indexOf(NESTED_EXCEPTION_MARKER);
            if (nested > 0) {
                String subMessage = message.substring(nested+NESTED_EXCEPTION_MARKER.length()+1);
                int start = subMessage.indexOf(NESTED_EXCEPTION_START);
                int end = subMessage.indexOf(NESTED_EXCEPTION_END);
                if (start > 0 && end > 0 && end > start) {
                    return new RuntimeException(subMessage.substring(start+NESTED_EXCEPTION_START.length()+1, end));
                }
            }
        }
        return exception;
    }

    //////////////////////////////
    // Hex and base64 converters
    /////////////////////////////

    public String bytesToBase64(byte[] b) {
        return bytesToBase64(b, false, false);
    }

    public String bytesToUrlBase64(byte[] b) {
        return bytesToBase64(b, false, true);
    }

    public String bytesToBase64(byte[] b, boolean pretty, boolean isUrl) {
        byte[] b64 = isUrl? Base64.getUrlEncoder().encode(b) : Base64.getEncoder().encode(b);
        String result = getUTF(b64);
        if (pretty) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (char c: result.toCharArray()) {
                sb.append(c);
                if (++i == 64) {
                    sb.append("\r\n");
                    i = 0;
                }
            }
            String prettyStr = sb.toString();
            result = prettyStr.endsWith("\n") ? prettyStr : sb+"\r\n";
        }
        return result;
    }

    public byte[] base64ToBytes(String base64) {
        return base64ToBytes(base64, false);
    }

    public byte[] urlBase64ToBytes(String base64) {
        return base64ToBytes(base64, true);
    }

    public byte[] base64ToBytes(String base64, boolean isUrl) {
        String b64 = base64.contains("\n") ?  base64.replace("\n", "")
                                                    .replace("\r", "") : base64;
        if (isUrl) {
            return Base64.getUrlDecoder().decode(b64);
        } else {
            return Base64.getDecoder().decode(b64);
        }
    }

    public byte[] hex2bytes(String hex) {
        char[] data = hex.toLowerCase().toCharArray();
        int len = data.length;
        if (len % 2 != 0) {
            throw new IllegalArgumentException(INVALID_HEX);
        }
        byte[] out = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            int f = getNumericValue(data[j]) << 4;
            j++;
            f = f | getNumericValue(data[j]);
            j++;
            out[i] = (byte) (f & 0xFF);
        }
        return out;
    }

    public String bytes2hex(byte[] bytes) {
        int l = bytes.length;
        char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = HEX_DIGITS[(0xF0 & bytes[i]) >>> 4 ];
            out[j++] = HEX_DIGITS[ 0x0F & bytes[i] ];
        }
        return String.valueOf(out);
    }

    private int getNumericValue(char c) {
        int result = Character.digit(c, 16);
        if (result == -1) {
            throw new IllegalArgumentException(INVALID_HEX);
        }
        return result;
    }

    //////////////////////////////////
    // Network connectivity utilities
    //////////////////////////////////

    public boolean portReady(String host, int port, int timeoutMs) {
        return testPort(host, port, timeoutMs);
    }

    private boolean testPort(String host, int port, int timeoutMs) {
        if (port < 1) {
            return false;
        }
        InetSocketAddress target = new InetSocketAddress(host, port);
        try (Socket s = new Socket()) {
            s.setReuseAddress(true);
            s.bind(null);
            s.connect(target, timeoutMs);
            return true;
        } catch (IOException te) {
            // ok to ignore
        }
        return false;
    }

    /**
     * This function checks if the IP-4 address is an intranet address
     * ("localhost" is not evaluated as intranet so that it can be used in unit tests)
     *
     * @param host IP-4 address
     * @return true if it is an intranet address
     */
    public boolean isIntranetAddress(String host) {
        Utility util = Utility.getInstance();
        if (host == null) {
            return false;
        }
        if (host.contains(":")) {
            host = host.substring(0, host.lastIndexOf(':'));
        }
        if ("127.0.0.1".equals(host)) {
            return true;
        }
        List<String> segments = util.split(host, ".");
        if (segments.size() != 4) {
            return false;
        }
        for (String number: segments) {
            if (number.length() > 3 || !util.isDigits(number)) {
                return false;
            }
        }
        for (String prefix: intranetPrefixes) {
            if (host.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public String elapsedTime(long milliseconds) {
        StringBuilder sb = new StringBuilder();
        long time = milliseconds;
        if (time > ONE_DAY_MS) {
            long days = time / ONE_DAY_MS;
            sb.append(days);
            sb.append(days == 1? " day " : " days ");
            time -= days * ONE_DAY_MS;
        }
        if (time > ONE_HOUR_MS) {
            long hours = time / ONE_HOUR_MS;
            sb.append(hours);
            sb.append(hours == 1? " hour " : " hours ");
            time -= hours * ONE_HOUR_MS;
        }
        if (time > ONE_MINUTE_MS) {
            long minutes = time / ONE_MINUTE_MS;
            sb.append(minutes);
            sb.append(minutes == 1? " minute " : " minutes ");
            time -= minutes * ONE_MINUTE_MS;
        }
        if (time >= ONE_SECOND_MS) {
            long seconds = time / ONE_SECOND_MS;
            sb.append(seconds);
            sb.append(seconds == 1 ? " second" : " seconds");
        }
        return sb.isEmpty() ? time + " ms" : sb.toString().trim();
    }

    public int getDurationInSeconds(String duration) {
        Utility util = Utility.getInstance();
        int multiplier = 1;
        final int n;
        if (duration.endsWith("s") || duration.endsWith("m") || duration.endsWith("h") || duration.endsWith("d")) {
            n = util.str2int(duration.substring(0, duration.length()-1).trim());
            if (duration.endsWith("m")) {
                multiplier = ONE_MINUTE;
            }
            if (duration.endsWith("h")) {
                multiplier = ONE_HOUR;
            }
            if (duration.endsWith("d")) {
                multiplier = ONE_DAY;
            }
        } else {
            n = util.str2int(duration);
        }
        return n * multiplier;
    }

    //////////////////////////////////////////////////////////////////
    // Convenient utility to close a connection of a WebSocketService
    //////////////////////////////////////////////////////////////////
    public void closeConnection(String txPath, int status, String message) {
        EventEmitter.getInstance().send(txPath, new Kv(WsEnvelope.TYPE, WsEnvelope.CLOSE),
                                              new Kv(STATUS, status), new Kv(MESSAGE, message));
    }

    //////////////////////////////////////////////////////
    // Convenient utilities to encode and decode URI path
    //////////////////////////////////////////////////////

    /**
     * Encode text as a URI
     *
     * @param text to be encoded
     * @return encoded uri
     */
    public String encodeUri(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Decode URI path
     *
     * @param uriPath of an HTTP request
     * @return decoded URI
     */
    public String getDecodedUri(String uriPath) {
        if (uriPath == null) {
            return "/";
        } else {
            // Decode URI escape characters
            var uri = uriPath.contains("%")? URLDecoder.decode(uriPath, StandardCharsets.UTF_8) : uriPath;
            // Avoid "path traversal" attack
            return uri.replace("\\", "/").replace("../", "");
        }
    }

    /**
     * Encode uri path with matrix, query and hash parameters
     *
     * @param uriPath to be encoded
     * @return encoded uri
     */
    public String getEncodedUri(String uriPath) {
        if (uriPath.contains("%")) {
            // no need to encode if the path is already encoded
            return uriPath;
        } else {
            final Map<String, List<String>> hashTags = new HashMap<>();
            final Map<String, List<String>> queryParams = new HashMap<>();
            var uri = uriPath;
            // encode hash parameters if any
            int hashMark = uri.lastIndexOf('#');
            if (hashMark != -1) {
                String qs = uri.substring(hashMark+1);
                uri = uri.substring(0, hashMark);
                hashTags.putAll(getUriParams(qs));
            }
            // encode query parameters if any
            int questionMark = uri.lastIndexOf('?');
            if (questionMark != -1) {
                String qs = uri.substring(questionMark+1);
                uri = uri.substring(0, questionMark);
                queryParams.putAll(getUriParams(qs));
            }
            // filter out protocol identifier
            uri = uri.replace("://", "/");
            // encode matrix parameters if any
            StringBuilder sb = new StringBuilder();
            List<String> segments = split(uri, "/");
            if (uri.contains(";")) {
                for (String s: segments) {
                    sb.append('/');
                    int semiColon = s.indexOf(';');
                    if (semiColon == -1) {
                        sb.append(encodeUri(s));
                    } else {
                        var pathPortion = s.substring(0, semiColon);
                        sb.append(encodeUri(pathPortion));
                        // matrix parameters
                        List<String> parts = split(s.substring(semiColon+1), ";");
                        for (String p: parts) {
                            sb.append(';');
                            int eq = p.indexOf('=');
                            if (eq == -1) {
                                sb.append(encodeUri(p));
                            } else {
                                sb.append(encodeUri(p.substring(0, eq)));
                                sb.append('=');
                                sb.append(encodeUri(p.substring(eq+1)));
                            }
                        }
                    }
                }
            } else {
                for (String s: segments) {
                    sb.append('/');
                    sb.append(encodeUri(s));
                }
            }
            if (uri.endsWith("/")) {
                sb.append('/');
            }
            uri = sb.toString();
            uri = queryParams.isEmpty()? uri : buildUriParams(uri, queryParams, true);
            return hashTags.isEmpty()? uri : buildUriParams(uri, hashTags, false);
        }
    }

    private Map<String, List<String>> getUriParams(String qs) {
        final Map<String, List<String>> query = new HashMap<>();
        List<String> parts = split(qs, "&");
        for (String p: parts) {
            int eq = p.indexOf('=');
            final String key = encodeUri(eq == -1? p : p.substring(0, eq));
            final String value = eq == -1? "" : encodeUri(p.substring(eq+1));
            List<String> params = query.getOrDefault(key, new ArrayList<>());
            params.add(value);
            query.put(key, params);
        }
        return query;
    }

    private static String buildUriParams(String uri, Map<String, List<String>> query, boolean isQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append(uri);
        sb.append(isQuery? '?' : '#');
        for (var entry: query.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String v: values) {
                sb.append(key);
                if (isQuery || !v.isEmpty()) {
                    sb.append('=');
                    sb.append(v);
                }
                sb.append('&');
            }
        }
        return sb.substring(0, sb.length()-1);
    }
}
