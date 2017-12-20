package com.github.mike10004.chromecookieimplant;

import io.github.mike10004.crxtool.CrxMetadata;
import io.github.mike10004.crxtool.CrxParser;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ChromeCookieImplanterStaticTest {

    @Test
    public void getCrxResourceOrDie() throws Exception {
        URL resource = ChromeCookieImplanter.getCrxResourceOrDie();
        assertNotNull(resource);
        CrxMetadata metadata;
        try (InputStream in = resource.openStream()) {
            metadata = CrxParser.getDefault().parseMetadata(in);
        }
        System.out.println(metadata);
    }

    @Test
    public void info() throws Exception {
        Properties p = new Properties();
        try (InputStream in = ChromeCookieImplanter.class.getResourceAsStream("/chrome-cookie-implant/info.properties")) {
            assertNotNull("resource input stream", in);
            p.load(in);
        }
        Stream.of("project.groupId", "project.artifactId", "project.version").forEach(key -> {
            String value = p.getProperty(key);
            assertFalse("starts with ${ - " + key, value.startsWith("${"));
        });
    }
}