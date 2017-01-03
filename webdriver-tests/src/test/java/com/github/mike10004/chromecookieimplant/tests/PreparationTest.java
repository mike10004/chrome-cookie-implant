package com.github.mike10004.chromecookieimplant.tests;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotNull;

public class PreparationTest {

    @Test
    public void makeWorked() throws Exception {
        URL resource = getClass().getResource("/chrome-cookie-implant.crx");
        assertNotNull("resource not in test output dir", resource);
    }

}
