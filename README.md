[![Travis build status](https://img.shields.io/travis/mike10004/chrome-cookie-implant.svg)](https://travis-ci.org/mike10004/chrome-cookie-implant)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.mike10004/chrome-cookie-implant.svg)](https://repo1.maven.org/maven2/com/github/mike10004/chrome-cookie-implant/)

Chrome Cookie Implant
=====================

This is a Chrome extension that allows you to execute an HTTP GET request to
add cookies to your Chrome profile. The request is made to a `chrome-extension://` URL
whose host is the the extension ID and whose query parameters contain URL-encoded 
JSON objects that contain the cookie data.

This is useful in web testing because the Chrome Extensions API for cookie 
management is more powerful than the WebDriver cookies API.

Using the extension
-------------------

You can use the provided Java client library to install the extension and 
implant cookies, or you can use the extension in any language by grabbing the 
CRX artifact, installing it, and making your own HTTP requests. This is some
example code that demonstrates using the extension without the client library:

    import io.github.bonigarcia.wdm.ChromeDriverManager;
    import org.apache.http.client.utils.URIBuilder;
    import org.openqa.selenium.chrome.ChromeDriver;
    import org.openqa.selenium.chrome.ChromeOptions;
    
    import java.io.File;
    import java.net.URI;
    
    public class CookieImplantExample {
    
        public static void main(String[] args) throws Exception {
            ChromeDriverManager.getInstance().setup();
            ChromeOptions options = new ChromeOptions();
            String extensionId = "neiaahbjfbepoclbammdhcailekhmcdm";
            options.addExtensions(new File("/home/mike10004/chrome-cookie-implant/" + extensionId + "-1.5.crx"));
            ChromeDriver driver = new ChromeDriver(options);
            String cookieJson = "{" +
                    "\"url\":\"http://www.example.com/\"," +
                    "\"domain\":\".www.example.com\"," +
                    "\"path\":\"/\"," +
                    "\"name\":\"my_cookie_name\"," +
                    "\"value\":\"my_cookie_value\"," +
                    "\"expirationDate\":1513292605.379" +
            "}";
            URI uri = new URIBuilder(URI.create("chrome-extension://" + extensionId + "/manage.html"))
                    .addParameter("implant", cookieJson)
                    .build();
            System.out.println(uri);
            driver.get(uri.toString());
        }
    
    }

See [Chrome cookies API](https://developer.chrome.com/extensions/cookies#method-set)
documentation on the properties the cookies can have.

Acknowledgements
----------------

Icon by Adèle Foucart, [CC 3.0](http://creativecommons.org/licenses/by/3.0/us/)
via [The Noun Project](https://thenounproject.com/term/chocolate-chip-cookie/261714/).

