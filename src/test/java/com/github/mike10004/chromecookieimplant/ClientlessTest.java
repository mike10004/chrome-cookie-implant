package com.github.mike10004.chromecookieimplant;

import com.google.gson.JsonParser;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ClientlessTest extends WebDriverTestBase {

    private static final boolean debug = false;

    @Test
    public void testSetNoCookies_raw() throws Exception {
        ChromeDriver driver = createDriver();
        try {
            URI uri = new URI("chrome-extension", getExtensionId(), "/manage.html", null, null);
            System.out.println(uri);
            driver.get(uri.toString());
            new WebDriverWait(driver, 3).until(outputStatusAllProcessed());
        } finally {
            driver.quit();
        }
    }

    private static Function<WebDriver, Boolean> outputStatusAllProcessed() {
        return new Function<WebDriver, Boolean>() {
            private final JsonParser jsonParser = new JsonParser();
            private final AtomicInteger pollCounter = new AtomicInteger(0);
            @Override
            public Boolean apply(WebDriver webDriver) {
                WebElement outputElement = new WebDriverWait(webDriver, 3)
                        .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#output")));
                String outputJson = outputElement.getText();
                if (debug) {
                    System.out.format("cookie implant extension output %d:", pollCounter.incrementAndGet());
                    System.out.println();
                    System.out.println(outputJson);
                    System.out.println();
                }
                String status = jsonParser.parse(outputJson).getAsJsonObject().get("status").getAsString();
                return CookieProcessingStatus.all_implants_processed.toString().equals(status);
            }
        };
    }

}
