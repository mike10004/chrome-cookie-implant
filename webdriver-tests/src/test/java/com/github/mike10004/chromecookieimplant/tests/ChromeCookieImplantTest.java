package com.github.mike10004.chromecookieimplant.tests;

import com.github.mike10004.xvfbselenium.WebDriverSupport;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.Function;
import com.google.common.math.LongMath;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("Guava")
public class ChromeCookieImplantTest {

    @Rule
    public XvfbRule xvfb = XvfbRule.builder().disabledOnWindows().build();

    @Before
    public void waitForDisplay() throws InterruptedException {
        xvfb.getController().waitUntilReady();
    }

    @BeforeClass
    public static void setUpDriver() {
        String driverPath = System.getProperty("webdriver.chrome.driver");
        if (driverPath == null) {
            ChromeDriverManager.getInstance().setup();
        }
    }

    private File getExtensionFile() throws FileNotFoundException, URISyntaxException {
        String resourcePath = "/chrome-cookie-implant.crx";
        URL resource = getClass().getResource(resourcePath);
        if (resource == null) {
            throw new FileNotFoundException("classpath:" + resourcePath);
        }
        return new File(resource.toURI());
    }

    private String extractExtensionId(File crxFile) throws IOException {
        return new CrxMetadataParser().parse(crxFile).id;
    }

    private BigDecimal generateExpiryDateInSeconds() {
        long millis = LongMath.checkedAdd(Instant.now().toEpochMilli(), 7 * 24 * 60 * 60 * 1000);
        return BigDecimal.valueOf(millis).scaleByPowerOfTen(-3);
    }

    @Test
    public void testSetCookie() throws Exception {
        ChromeOptions options = new ChromeOptions();
        File extensionFile = getExtensionFile();
        String extensionId = extractExtensionId(extensionFile);
        options.addExtensions(extensionFile);
        ChromeDriver driver = WebDriverSupport.chromeInEnvironment(xvfb.getController().configureEnvironment(new HashMap<>())).create(options);
        try {
            String cookieJson = "{" +
                    "\"url\":\"http://httpbin.org/\"," +
                    "\"domain\":\".httpbin.org\"," +
                    "\"path\":\"/\"," +
                    "\"name\":\"foo\"," +
                    "\"value\":\"bar\"," +
                    "\"expirationDate\":" + generateExpiryDateInSeconds().toString() +
                    "}";
            System.out.println(cookieJson);
            URI uri = new URI("chrome-extension", extensionId, "/manage.html", "import=" + cookieJson, null);
            System.out.println(uri);
            driver.get(uri.toString());
            String status = new WebDriverWait(driver, 3).until(outputStatusNotNotYetProcessed());
            assertEquals("status", "all_imports_processed", status);
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

    private static Function<WebDriver, String> outputStatusNotNotYetProcessed() {
        return new Function<WebDriver, String>() {
            private final JsonParser jsonParser = new JsonParser();
            @Override
            public String apply(WebDriver webDriver) {
                WebElement outputElement = new WebDriverWait(webDriver, 3).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#output")));
                String outputJson = outputElement.getText();
                String status = jsonParser.parse(outputJson).getAsJsonObject().get("status").getAsString();
                if ("not_yet_processed".equals(status)) {
                    return null; // keep waiting
                } else {
                    return status;
                }
            }
        };
    }

}