package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class StringUtilsTest {

    static final char ch = '*';

    @Test
    void test_getMaxSizeString_null() {
        assertNull(null, StringUtils.getMaxSizeString(null));
    }

    @Test
    void test_getMaxSizeString_short_string() {
        assertEquals("aaaaa", StringUtils.getMaxSizeString("aaaaa"));
    }

    @Test
    void test_getMaxSizeString_long_string() {
        char[] charArray = new char[1500];
        Arrays.fill(new char[1500], ch);
        String res = StringUtils.getMaxSizeString(new String(charArray));
        assertEquals(1024, res.length());
    }

    @Test
    void test_randomStringAndNumbers_check_size() {
        assertEquals(5, StringUtils.randomStringAndNumbers(5).length());
    }

    @Test
    void test_randomStringAndNumbers_check_only_contain_letters_and_numbers() {
        assertTrue(StringUtils.randomStringAndNumbers(50).matches("[a-z0-9]*"));
    }

    @Test
    void test_randomStringAndNumbers_check_random() {
        assertNotEquals(
                StringUtils.randomStringAndNumbers(50), StringUtils.randomStringAndNumbers(50));
    }

    @Test
    void extractStringForStream_long() throws IOException {
        InputStream inputStream =
                new ByteArrayInputStream("123456789".getBytes(Charset.forName("UTF-8")));
        assertEquals("12345", StringUtils.extractStringForStream(inputStream, 5));
        assertEquals("123456789", IOUtils.toString(inputStream, Charset.forName("UTF-8")));
    }

    @Test
    void extractStringForStream_short() throws IOException {
        InputStream inputStream =
                new ByteArrayInputStream("123456789".getBytes(Charset.forName("UTF-8")));
        assertEquals("123456789", StringUtils.extractStringForStream(inputStream, 100));
        assertEquals("123456789", IOUtils.toString(inputStream, Charset.forName("UTF-8")));
    }

    @Test
    void extractStringForStream_empty_stream() throws IOException {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        assertNull(StringUtils.extractStringForStream(empty, 100));
    }
}
