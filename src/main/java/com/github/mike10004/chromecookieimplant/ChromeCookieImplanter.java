package com.github.mike10004.chromecookieimplant;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import io.github.mike10004.crxtool.CrxParser;
import org.apache.http.client.utils.URIBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Service class that implants cookies into a Chrome webdriver instance.
 */
public class ChromeCookieImplanter {

    public static final String QUERY_PARAM_IMPLANT = "implant";
    @VisibleForTesting
    static final String EXTENSION_RESOURCE_PATH = "/chrome-cookie-implant.crx";
    private static final String IGNORED_PREFIX = "ignored:";
    public static final int DEFAULT_OUTPUT_TIMEOUT_SECONDS = 3;
    private static final Logger log = LoggerFactory.getLogger(ChromeCookieImplanter.class);

    private final ByteSource crxBytes;
    private transient final Gson gson;
    private final int outputTimeoutSeconds;
    private final Supplier<String> extensionIdSupplier;

    public ChromeCookieImplanter() {
        this(Resources.asByteSource(getCrxResourceOrDie()));
    }

    @VisibleForTesting
    protected ChromeCookieImplanter(ByteSource crxBytes) {
        this(crxBytes, DEFAULT_OUTPUT_TIMEOUT_SECONDS, new Gson());
    }

    protected ChromeCookieImplanter(ByteSource crxBytes, int outputTimeoutSeconds, Gson gson) {
        this.crxBytes = requireNonNull(crxBytes, "crxBytes");
        this.gson = requireNonNull(gson, "gson");
        this.outputTimeoutSeconds = outputTimeoutSeconds;
        checkArgument(outputTimeoutSeconds >= 0, "outputTimeoutSeconds >= 0 required: %s", outputTimeoutSeconds);
        extensionIdSupplier = Suppliers.memoize(() -> {
            try (InputStream in = crxBytes.openStream()){
                return CrxParser.getDefault().parseMetadata(in).id;
            } catch (IOException e) {
                throw new RuntimeException("failed to parse chrome extension metadata from .crx bytes", e);
            }
        });
    }

    static URL getCrxResourceOrDie() throws IllegalStateException {
        URL url = ChromeCookieImplanter.class.getResource(EXTENSION_RESOURCE_PATH);
        if (url == null) {
            throw new IllegalStateException("resource does not exist: classpath:/chrome-cookie-implant.crx");
        }
        return url;
    }

    /**
     * Copies the extension package to the given output stream.
     * @param outputStream the output stream
     * @throws IOException if copying fails
     */
    public void copyCrxTo(OutputStream outputStream) throws IOException {
        crxBytes.copyTo(outputStream);
    }

    /**
     * Interface that defines a method to handle a cookie implant result.
     */
    public interface ResultExaminer {
        /**
         * Examine the result
         * @param result the result
         * @throws CookieImplantException depending on whether the underlying implementation
         */
        void examine(CookieImplantResult result) throws CookieImplantException;

        /**
         * Creates and returns a default result examiner implementation instance.
         * The implementation throws a {@link CookieImplantException} on most types of
         * implant failures. Some implant failures are ignored, such as those that are
         * due to cookie expiration dates in the past.
         * @return a result examiner
         */
        static ResultExaminer createDefault() {
            return DefaultResultExaminer.INSTANCE;
        }
    }

    private static class DefaultResultExaminer implements ResultExaminer {

        private static final DefaultResultExaminer INSTANCE = new DefaultResultExaminer();

        @Override
        public void examine(CookieImplantResult result) {
            if (!result.success) {
                if (result.message != null && result.message.startsWith(IGNORED_PREFIX)) {
                    log.info("cookie implant ignored: index={}, message={}", result.index, result.message);
                } else {
                    log.error("cookie implant failed: index={}, message={}", result.index, result.message);
                    throw new CookieImplantException("cookie " + result.index + " failed to be implanted: " + result.message);
                }
            }
        }

    }

    /**
     * Implants a collection of cookies into a Chrome webdriver instance. The default
     * result examiner is used, and it throws an exception on most implant failures.
     * @param cookies the cookies
     * @param driver the webdriver
     * @return the results
     * @see ResultExaminer#createDefault()
     */
    public ImmutableList<CookieImplantResult> implant(Collection<ChromeCookie> cookies, ChromeDriver driver) {
        return implant(cookies, driver, ResultExaminer.createDefault());
    }

    /**
     * Attempts to implant a collection of cookies, reacting to each attempt using the given
     * result examiner.
     * @param cookies the cookies
     * @param driver the webdriver
     * @param resultExaminer the result examiner
     * @return the results
     */
    public ImmutableList<CookieImplantResult> implant(Collection<ChromeCookie> cookies, ChromeDriver driver, ResultExaminer resultExaminer) {
        requireNonNull(resultExaminer, "failureHandler");
        URI manageUrl = buildImplantUriFromCookies(cookies.stream());
        driver.get(manageUrl.toString());
        CookieImplantOutput output = waitForCookieImplantOutput(driver, outputTimeoutSeconds);
        int numFailures = 0;
        for (CookieImplantResult result : output.implants) {
            if (!result.success) {
                numFailures++;
            }
            resultExaminer.examine(result);
        }
        log.debug("{} of {} cookies implanted using implant extension", cookies.size() - numFailures, cookies.size());
        return ImmutableList.copyOf(output.implants);
    }

    protected <T> By elementTextRepresentsObject(By elementLocator, Class<T> deserializedType, Predicate<? super T> predicate) {
        return new By() {
            @Override
            public List<WebElement> findElements(SearchContext context) {
                List<WebElement> parents = elementLocator.findElements(context);
                List<WebElement> filteredElements = new ArrayList<>(parents.size());
                for (WebElement parent : parents) {
                    String json = parent.getText();
                    T item = gson.fromJson(json, deserializedType);
                    if (predicate.test(item)) {
                        filteredElements.add(parent);
                    }
                }
                return filteredElements;
            }
        };
    }

    protected By byOutputStatus(Predicate<CookieProcessingStatus> statusPredicate) {
        return elementTextRepresentsObject(By.cssSelector("#output"), CookieImplantOutput.class, cio -> statusPredicate.test(cio.status));
    }

    protected CookieImplantOutput waitForCookieImplantOutput(WebDriver driver, int timeOutInSeconds) {
        By by = byOutputStatus(CookieProcessingStatus.all_implants_processed::equals);
        Function<? super WebDriver, WebElement> fn = ExpectedConditions.presenceOfElementLocated(by);
        WebElement outputElement = new WebDriverWait(driver, timeOutInSeconds)
                .until(fn);
        String outputJson = outputElement.getText();
        CookieImplantOutput output = gson.fromJson(outputJson, CookieImplantOutput.class);
        return output;
    }


    protected URI buildImplantUriFromCookies(Stream<ChromeCookie> cookies) {
        return buildImplantUriFromCookieJsons(cookies
                .map(gson::toJson));
    }

    protected String getExtensionId() {
        return extensionIdSupplier.get();
    }

    protected URI buildImplantUriFromCookieJsons(Stream<String> cookieJsons) {
        try {
            URIBuilder uriBuilder = new URIBuilder(URI.create("chrome-extension://" + getExtensionId() + "/manage.html"));
            cookieJsons.forEach(cookieJson -> uriBuilder.addParameter(QUERY_PARAM_IMPLANT, cookieJson));
            URI uri = uriBuilder.build();
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }

    }

}
