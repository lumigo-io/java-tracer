package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
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
}
