/*
 * (c) 2017 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.chromecookieimplant;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.gson.Gson;
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
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChromeCookieImplanter {

    private static final Logger log = LoggerFactory.getLogger(ChromeCookieImplanter.class);

    private final ByteSource crxBytes;
    private transient final Gson gson;
    private final int outputTimeoutSeconds;
    private final java.util.function.Supplier<String> extensionIdSupplier;

    public ChromeCookieImplanter() {
        this(Resources.asByteSource(getCrxResourceOrDie()));
    }

    @VisibleForTesting
    ChromeCookieImplanter(ByteSource crxBytes) {
        this.crxBytes = crxBytes;
        gson = new Gson();
        outputTimeoutSeconds = 3;
        extensionIdSupplier = Suppliers.memoize(() -> {
            try {
                return new CrxMetadataParser().parse(crxBytes).id;
            } catch (IOException e) {
                throw new RuntimeException("failed to parse chrome extension metadata from .crx bytes", e);
            }
        })::get;
    }

    private static URL getCrxResourceOrDie() throws IllegalStateException {
        URL url = ChromeCookieImplanter.class.getResource("/chrome-cookie-implant.crx");
        if (url == null) {
            throw new IllegalStateException("resource does not exist: classpath:/chrome-cookie-implant.crx");
        }
        return url;
    }

    public void copyCrxTo(OutputStream outputStream) throws IOException {
        crxBytes.copyTo(outputStream);
    }

    public interface FailureHandler {
        void handleFailure(CookieImplantResult result);
    }

    private static class DefaultFailureHandler implements FailureHandler {

        @Override
        public void handleFailure(CookieImplantResult result) {
            log.error("cookie implant failed: {} {}", result.index, result.message);
            throw new CookieImplantException("cookie " + result.index + " failed to be implanted: " + result.message);
        }

        private static final DefaultFailureHandler instance = new DefaultFailureHandler();
    }

    public ImmutableList<CookieImplantResult> implant(Collection<ChromeCookie> cookies, ChromeDriver driver) {
        return implant(cookies, driver, DefaultFailureHandler.instance);
    }

    public ImmutableList<CookieImplantResult> implant(Collection<ChromeCookie> cookies, ChromeDriver driver, FailureHandler failureHandler) {
        checkNotNull(failureHandler, "failureHandler");
        URI manageUrl = buildImplantUriFromCookies(cookies.stream());
        driver.get(manageUrl.toString());
        CookieImplantOutput output = waitForCookieImplantOutput(driver, outputTimeoutSeconds);
        int numFailures = 0;
        for (CookieImplantResult result : output.imports) {
            if (!result.success) {
                numFailures++;
                failureHandler.handleFailure(result);
            }
        }
        log.debug("{} of {} cookies imported using implant extension", cookies.size() - numFailures, cookies.size());
        return ImmutableList.copyOf(output.imports);
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

    protected By byOutputStatus(Predicate<CookieImplantStatus> statusPredicate) {
        return elementTextRepresentsObject(By.cssSelector("#output"), CookieImplantOutput.class, cio -> statusPredicate.test(cio.status));
    }

    protected CookieImplantOutput waitForCookieImplantOutput(WebDriver driver, int timeOutInSeconds) {
        WebElement outputElement = new WebDriverWait(driver, timeOutInSeconds)
                .until(ExpectedConditions.presenceOfElementLocated(byOutputStatus(CookieImplantStatus.all_imports_processed::equals)));
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
            cookieJsons.forEach(cookieJson -> uriBuilder.addParameter("import", cookieJson));
            URI uri = uriBuilder.build();
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }

    }
}