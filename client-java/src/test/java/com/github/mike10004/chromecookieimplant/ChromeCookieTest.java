package com.github.mike10004.chromecookieimplant;

import com.github.mike10004.chromecookieimplant.ChromeCookie.SameSiteStatus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class ChromeCookieTest {

    @Test
    public void deserialize() throws Exception {
        JsonObject object = new JsonObject();
        object.addProperty("name", "foo");
        BigDecimal expiry = new BigDecimal("1484670334.671");
        object.add("expirationDate", new JsonPrimitive(expiry));
        ChromeCookie cookie = new Gson().fromJson(object, ChromeCookie.class);
        assertEquals("name", "foo", cookie.name);
        assertEquals("expirationDate", expiry, cookie.expirationDate);
    }

    @Test
    public void serialize() throws Exception {
        BigDecimal expiry = new BigDecimal("1484670334.671");
        ChromeCookie cookie = ChromeCookie.builder("https://www.example.com/")
                .sameSite(SameSiteStatus.lax)
                .name("foo")
                .value("bar")
                .expirationDate(expiry)
                .build();
        Gson gson = new Gson();
        String json = gson.toJson(cookie);
        ChromeCookie d = gson.fromJson(json, ChromeCookie.class);
        assertEquals("name", cookie.name, d.name);
        assertEquals("value", cookie.value, d.value);
        assertEquals("sameSite", cookie.sameSite, d.sameSite);
        assertEquals("url", cookie.url, d.url);

    }

}