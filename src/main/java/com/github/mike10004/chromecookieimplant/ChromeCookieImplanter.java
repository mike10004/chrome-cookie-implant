package com.github.mike10004.chromecookieimplant;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import io.github.mike10004.crxtool.CrxMetadata;
import io.github.mike10004.crxtool.CrxParser;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Service class that implants cookies into a Chrome webdriver instance.
 */
public class ChromeCookieImplanter {

    public static final String QUERY_PARAM_IMPLANT = "implant";
    @VisibleForTesting
    static final String EXTENSION_RESOURCE_PATH = "/chrome-cookie-implant.crx";
    private static final Gson DEFAULT_GSON = new Gson();
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
        this(crxBytes, DEFAULT_OUTPUT_TIMEOUT_SECONDS, DEFAULT_GSON);
    }

    protected ChromeCookieImplanter(ByteSource crxBytes, int outputTimeoutSeconds, Gson gson) {
        this.crxBytes = requireNonNull(crxBytes, "crxBytes");
        this.gson = requireNonNull(gson, "gson");
        this.outputTimeoutSeconds = outputTimeoutSeconds;
        checkArgument(outputTimeoutSeconds >= 0, "outputTimeoutSeconds >= 0 required: %s", outputTimeoutSeconds);
        extensionIdSupplier = Suppliers.memoize(() -> {
            try (InputStream in = ChromeCookieImplanter.this.crxBytes.openStream()){
                CrxMetadata metadata = CrxParser.getDefault().parseMetadata(in);
                return metadata.getId();
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
    public List<CookieImplantResult> implant(Collection<ChromeCookie> cookies, WebDriver driver) {
        return implant(cookies, driver, ResultExaminer.createDefault());
    }

    /**
     * Attempts to implant a collection of cookies, reacting to each attempt using the given
     * result examiner.
     * @param cookies the cookies
     * @param driver the webdriver
     * @param resultExaminer the result examiner
     * @return an immutable list of results
     */
    public List<CookieImplantResult> implant(Collection<ChromeCookie> cookies, WebDriver driver, ResultExaminer resultExaminer) {
        requireNonNull(resultExaminer, "failureHandler");
        if (cookies.isEmpty()) {
            return Collections.emptyList();
        }
        URI manageUrl = buildImplantUriFromCookies(new ArrayList<>(cookies));
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
        return Collections.unmodifiableList(output.implants);
    }

    @SuppressWarnings("SameParameterValue")
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

    protected String getExtensionId() {
        return extensionIdSupplier.get();
    }

    private static final String CHROME_EXTENSION_SCHEME = "chrome-extension";
    private static final String IMPLANT_URL_PATH = "/manage.html";
    private static final String URL_ENCODING_CHARSET = StandardCharsets.UTF_8.name();

    /**
     * Builds the implant URI from a cookie list.
     * @param cookies cookies list; must be nonempty
     * @return the URI
     */
    protected URI buildImplantUriFromCookies(List<ChromeCookie> cookies) {
        checkArgument(!cookies.isEmpty(), "cookies list must be nonempty");
        try {
            StringBuilder s = new StringBuilder(512);
            String host = getExtensionId();
            s.append(CHROME_EXTENSION_SCHEME)
                    .append("://")
                    .append(host)
                    .append(IMPLANT_URL_PATH)
                    .append('?');
            for (int i = 0; i < cookies.size(); i++) {
                if (i > 0) {
                    s.append('&');
                }
                ChromeCookie cookie = cookies.get(i);
                String json = gson.toJson(cookie);
                s.append(QUERY_PARAM_IMPLANT).append('='); // we know the param name does not need escaping
                try {
                    String paramValue = URLEncoder.encode(json, URL_ENCODING_CHARSET);
                    s.append(paramValue);
                } catch (UnsupportedEncodingException e) {
                    // JRE must support US_ASCII
                    throw new RuntimeException(e);
                }
            }
            URI uri = new URI(s.toString());
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }

    }

}
