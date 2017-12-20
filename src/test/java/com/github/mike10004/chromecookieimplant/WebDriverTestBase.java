package com.github.mike10004.chromecookieimplant;

import com.github.mike10004.xvfbtesting.XvfbRule;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.mike10004.crxtool.BasicCrxParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

public class WebDriverTestBase {

    private static File extensionFile;
    private static String extensionId;

    @ClassRule
    public static final TemporaryFolder classTempFolder = new TemporaryFolder();

    @BeforeClass
    public static void prepareCrxFile() throws IOException {
        File destination = classTempFolder.newFile("chrome-cookie-implant.crx");
        try (OutputStream out = new FileOutputStream(destination)) {
            new ChromeCookieImplanter().copyCrxTo(out);
        }
        extensionFile = destination;
        extensionId = extractExtensionId(extensionFile);
    }

    @Rule
    public XvfbRule xvfb = XvfbRule.builder().build();

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

    protected static String getExtensionId() {
        return extensionId;
    }

    private static String extractExtensionId(File crxFile) throws IOException {
        try (InputStream input = new FileInputStream(crxFile)) {
            return new BasicCrxParser().parseMetadata(input).id;
        }
    }

    @SuppressWarnings("RedundantThrows")
    protected ChromeDriver createDriver() throws IOException, URISyntaxException {
        ChromeOptions options = new ChromeOptions();
        options.addExtensions(extensionFile);
        ChromeDriverService service = new ChromeDriverService.Builder()
                .withEnvironment(xvfb.getController().newEnvironment())
                .build();
        ChromeDriver driver = new ChromeDriver(service, options);
        return driver;
    }

}
