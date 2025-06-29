package com.example;

import org.junit.Test;
import java.io.FileInputStream;

public class MyTest {
    @Test
    public void testSomething() throws Exception {
        new FileInputStream("config.properties");
    }
} 