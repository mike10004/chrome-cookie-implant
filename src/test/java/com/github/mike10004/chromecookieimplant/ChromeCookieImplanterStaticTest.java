package com.github.mike10004.chromecookieimplant;

import io.github.mike10004.crxtool.CrxMetadata;
import io.github.mike10004.crxtool.CrxParser;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ChromeCookieImplanterStaticTest {

    @Test
    public void getCrxResourceOrDie() throws Exception {
        System.out.println(readMetadata());
    }

    private CrxMetadata readMetadata() throws IOException {
        URL resource = ChromeCookieImplanter.getCrxResourceOrDie();
        assertNotNull(resource);
        try (InputStream in = resource.openStream()) {
            return CrxParser.getDefault().parseMetadata(in);
        }
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

    @Test
    public void buildImplantUriFromCookieJsons() throws Exception {
        ChromeCookie cookie1 = ChromeCookie.builder("https://example.com/")
                .name("ooga_booga")
                .value("This/can&be:a(tad)complex")
                .build();
        ChromeCookie cookie2 = ChromeCookie.builder("https://elpmaxe.com/")
                .name("booga_ooga")
                .value("\uD83C\uDCA1\t\uD83C\uDCA2\t\uD83C\uDCA3\t\uD83C\uDCA4\t\uD83C\uDCA5\t\uD83C\uDCA6\t\uD83C\uDCA7\t\uD83C\uDCA8\t\uD83C\uDCA9\t\uD83C\uDCAA\t\uD83C\uDCAB\t\uD83C\uDCAC\t\uD83C\uDCAD\t\uD83C\uDCAE")
                .build();
        URI uri = new ChromeCookieImplanter().buildImplantUriFromCookies(Arrays.asList(cookie1, cookie2));
        String expected = "chrome-extension://kaoadjmhchcekjlnhdmeennkgjeacdio/manage.html?implant=%7B%22url%22%3A%22https%3A%2F%2Fexample.com%2F%22%2C%22name%22%3A%22ooga_booga%22%2C%22value%22%3A%22This%2Fcan%5Cu0026be%3Aa%28tad%29complex%22%7D&implant=%7B%22url%22%3A%22https%3A%2F%2Felpmaxe.com%2F%22%2C%22name%22%3A%22booga_ooga%22%2C%22value%22%3A%22%F0%9F%82%A1%5Ct%F0%9F%82%A2%5Ct%F0%9F%82%A3%5Ct%F0%9F%82%A4%5Ct%F0%9F%82%A5%5Ct%F0%9F%82%A6%5Ct%F0%9F%82%A7%5Ct%F0%9F%82%A8%5Ct%F0%9F%82%A9%5Ct%F0%9F%82%AA%5Ct%F0%9F%82%AB%5Ct%F0%9F%82%AC%5Ct%F0%9F%82%AD%5Ct%F0%9F%82%AE%22%7D";
        System.out.println(uri);
        assertEquals("URI build from cookie jsons", expected, uri.toString());
    }
}