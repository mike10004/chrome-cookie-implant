package com.github.mike10004.chromecookieimplant;

import com.github.mike10004.xvfbmanager.DefaultXvfbController;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
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
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class WebDriverTestBase {

    private static final String SYSPROP_CHROME_EXTRA_ARGS = "chrome-cookie-implant.chrome.extraArgs";
    private static final String SYSPROP_XVFB_WAIT_MILLIS = "chrome-cookie-implant.xvfb.waitMillis";
    private static final int DEFAULT_XVFB_WAIT_MILLIS = 2000;
    private static final long XVFB_POLL_INTERVAL_MILLIS = DefaultXvfbController.DEFAULT_POLL_INTERVAL_MS;

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
    public XvfbRule xvfb = XvfbRule.builder()
            .disabled(WebDriverTestBase::isVirtualDisplayDisabled)
            .build();

    @Before
    public void waitForDisplay() throws InterruptedException {
        int maxNumPolls = getXvfbMaxNumPolls();
        xvfb.getController().waitUntilReady(XVFB_POLL_INTERVAL_MILLIS, maxNumPolls);
    }

    private static int getXvfbMaxNumPolls() {
        String override = System.getProperty(SYSPROP_XVFB_WAIT_MILLIS);
        int waitMillis = DEFAULT_XVFB_WAIT_MILLIS;
        if (!Strings.isNullOrEmpty(override)) {
            try {
                waitMillis = Integer.parseInt(override);
            } catch (NumberFormatException e) {
                LoggerFactory.getLogger(WebDriverTestBase.class).warn("failed to parse user-specified xvfb wait", e);
            }
        }
        int numPolls = Math.round((float) waitMillis / (float) XVFB_POLL_INTERVAL_MILLIS);
        return numPolls;
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
            return new BasicCrxParser().parseMetadata(input).getId();
        }
    }

    @SuppressWarnings("RedundantThrows")
    protected ChromeDriver createDriver() throws IOException, URISyntaxException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(getChromeExtraArgs());
        options.addExtensions(extensionFile);
        Map<String, String> environment = xvfb.getController().newEnvironment();
        ChromeDriverService service = new ChromeDriverService.Builder()
                .withEnvironment(environment)
                .build();
        ChromeDriver driver = new ChromeDriver(service, options);
        return driver;
    }

    private List<String> getChromeExtraArgs() {
        String tokenStr = Strings.nullToEmpty(System.getProperty(SYSPROP_CHROME_EXTRA_ARGS));
        return Splitter.on(CharMatcher.breakingWhitespace()).trimResults().omitEmptyStrings().splitToList(tokenStr);
    }

    private static boolean isVirtualDisplayDisabled() {
        String val = System.getProperty("java.awt.headless");
        return !Boolean.parseBoolean(val);
    }
}
