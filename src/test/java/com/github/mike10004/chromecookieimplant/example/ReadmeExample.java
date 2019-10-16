package com.github.mike10004.chromecookieimplant.example;

import com.github.mike10004.chromecookieimplant.ChromeCookie;
import com.github.mike10004.chromecookieimplant.ChromeCookieImplanter;
import com.github.mike10004.chromecookieimplant.CookieImplantResult;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;

public class ReadmeExample {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws Exception {
        WebDriverManager.chromedriver().setup();
        File crxFile = File.createTempFile("chrome-cookie-implant", ".crx");
        ChromeCookieImplanter implanter = new ChromeCookieImplanter();
        try (OutputStream out = new FileOutputStream(crxFile)) {
            implanter.copyCrxTo(out);
        }
        ChromeOptions options = new ChromeOptions();
        options.addExtensions(crxFile);
        ChromeDriver driver = new ChromeDriver(options);
        ChromeCookie cookie = ChromeCookie.builder("https://www.example.com/")
                .name("my_cookie_name")
                .value("my_cookie_value")
                .build();
        CookieImplantResult result = implanter.implant(Collections.singleton(cookie), driver).get(0);
        System.out.println(result);
        driver.quit();
        crxFile.delete();
    }
}
