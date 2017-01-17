package com.github.mike10004.chromecookieimplant;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotNull;

public class ResourceTest {

    @Test
    public void testPluginBuiltExtensionInClasspath() throws Exception {
        URL resource = getClass().getResource("/chrome-cookie-implant.crx");
        assertNotNull("resource not in test output dir", resource);
    }

}
