package com.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JsonUtilsTest {

  @Test
  void testGetObjectAsJsonString_null() {
    assertNull(null, JsonUtils.getObjectAsJsonString(null));
  }

  @Test
  void testGetObjectAsJsonString_Object() {
    assertEquals("{\"a\":1,\"b\":\"2\"}", JsonUtils.getObjectAsJsonString(new A()));
  }

  @Test
  void testGetObjectAsJsonString_string() {
    assertEquals("aaa", JsonUtils.getObjectAsJsonString("aaa"));
  }

  @Test
  void testGetObjectAsJsonString_int() {
    assertEquals("1", JsonUtils.getObjectAsJsonString(1));
  }

  public class A {
    private int a = 1;
    private String b = "2";

    public int getA() {
      return a;
    }

    public String getB() {
      return b;
    }
  }
}
