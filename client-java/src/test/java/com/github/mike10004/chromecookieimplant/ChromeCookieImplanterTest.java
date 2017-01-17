package com.github.mike10004.chromecookieimplant;

import com.google.common.io.Files;
import com.google.common.math.LongMath;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChromeCookieImplanterTest extends WebDriverTestBase {

    @Test
    public void implant_single_valid() throws Exception {
        ChromeDriver driver = createDriver();
        try {
            ChromeCookie cookie = ChromeCookie.builder("http://httpbin.org/")
                    .domain(".httpbin.org")
                    .path("/")
                    .name("foo")
                    .value("bar")
                    .expirationDate(getDateInSecondsWithDaysOffset(7))
                    .build();
            System.out.println(cookie);
            ChromeCookieImplanter implanter = new ChromeCookieImplanter();
            List<CookieImplantResult> results = implanter.implant(Collections.singleton(cookie), driver);
            assertEquals("results length", 1, results.size());
            CookieImplantResult result = results.get(0);
            assertEquals("result.success", true, result.success);
            // now visit httpbin.org and confirm that the cookie is automatically sent by the browser
            driver.get("http://httpbin.org/get");
            String httpbinHtml = driver.getPageSource();
            String httpbinJson = Jsoup.parse(httpbinHtml).getElementsByTag("body").first().text();
            System.out.println(httpbinJson);
            JsonObject httpbinResponse = new JsonParser().parse(httpbinJson).getAsJsonObject();
            JsonElement cookieHeader = httpbinResponse.get("headers").getAsJsonObject().get("Cookie");
            assertNotNull("cookie header", cookieHeader);
            String cookieHeaderValue = cookieHeader.getAsString();
            System.out.println("cookie header value: " + cookieHeaderValue);
            assertEquals("cookie header value", "foo=bar", cookieHeaderValue);
        } finally {
            driver.quit();
        }
    }

    /**
     * Generates a date
     * @return
     */
    private BigDecimal getDateInSecondsWithDaysOffset(int daysOffset) {
        long nowMs = Instant.now().toEpochMilli();
        long daysMs = LongMath.checkedMultiply(daysOffset, 24 * 60 * 60 * 1000);
        return BigDecimal.valueOf(LongMath.checkedAdd(nowMs, daysMs)).scaleByPowerOfTen(-3);
    }

    @Test
    public void implant_single_expired_ignore() throws Exception {
        ChromeDriver driver = createDriver();
        BigDecimal expiryInSeconds = getDateInSecondsWithDaysOffset(-7);
        try {
            ChromeCookie cookie = ChromeCookie.builder("https://www.example.com/")
                    .name("foo")
                    .expirationDate(expiryInSeconds)
                    .build();
            System.out.println(cookie);
            ChromeCookieImplanter implanter = new ChromeCookieImplanter();
            List<CookieImplantResult> results = implanter.implant(Collections.singleton(cookie), driver);
            assertEquals("results length", 1, results.size());
            CookieImplantResult result = results.get(0);
            assertEquals("result.success", false, result.success);
            assertEquals("result.message", "ignored:expired", result.message);
        } finally {
            driver.quit();
        }
    }

}